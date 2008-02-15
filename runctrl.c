/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/

/*
 * Target service implementation: run control (TCF name RunControl)
 */

#include "config.h"
#if SERVICE_RunControl

#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <signal.h>
#include <errno.h>
#include <assert.h>
#include "runctrl.h"
#include "protocol.h"
#include "channel.h"
#include "json.h"
#include "context.h"
#include "myalloc.h"
#include "trace.h"
#include "events.h"
#include "exceptions.h"
#include "breakpoints.h"

#define RM_RESUME           0
#define RM_STEP_OVER        1
#define RM_STEP_INTO        2
#define RM_STEP_OVER_LINE   3
#define RM_STEP_INTO_LINE   4
#define RM_STEP_OUT         5

#define STOP_ALL_TIMEOUT 1000000
#define STOP_ALL_MAX_CNT 20

static const char RUN_CONTROL[] = "RunControl";
static TCFSuspendGroup * suspend_group = NULL;

typedef struct SafeEvent SafeEvent;

struct SafeEvent {
    void (*done)(void *);
    void * arg;
    SafeEvent * next;
};

typedef struct GetContextArgs GetContextArgs;

struct GetContextArgs {
    Channel * c;
    char token[256];
    Context * ctx;
    pid_t parent;
};

static SafeEvent * safe_event_list = NULL;
static int safe_event_pid_count = 0;
static int safe_event_generation = 0;

#if !defined(WIN32) && !defined(_WRS_KERNEL)
static char *get_executable(pid_t pid) {
    static char s[FILE_PATH_SIZE + 1];
    char tmpbuf[100];
    int sz;

    snprintf(tmpbuf, sizeof(tmpbuf), "/proc/%d/exe", pid);
    if ((sz = readlink(tmpbuf, s, FILE_PATH_SIZE)) < 0) {
        trace(LOG_ALWAYS, "error: readlink() failed; pid %d, error %d %s",
            pid, errno, errno_to_str(errno));
        return NULL;
    }
    s[sz] = 0;
    return s;
}
#endif

static void write_context(OutputStream * out, Context * ctx, int is_thread) {
    assert(!ctx->exited);

    out->write(out, '{');

    json_write_string(out, "ID");
    out->write(out, ':');
    json_write_string(out, is_thread ? thread_id(ctx) : container_id(ctx));

    if (is_thread) {
        out->write(out, ',');
        json_write_string(out, "ParentID");
        out->write(out, ':');
        json_write_string(out, container_id(ctx));
    }

#if !defined(_WRS_KERNEL)
    out->write(out, ',');
    json_write_string(out, "ProcessID");
    out->write(out, ':');
    json_write_string(out, pid2id(ctx->mem, 0));
#endif
    
#if !defined(WIN32) && !defined(_WRS_KERNEL)
    if (!ctx->exiting && !is_thread) {
        out->write(out, ',');
        json_write_string(out, "File");
        out->write(out, ':');
        json_write_string(out, get_executable(ctx->pid));
    }
#endif

    if (is_thread) {
        out->write(out, ',');
        json_write_string(out, "CanSuspend");
        out->write(out, ':');
        json_write_boolean(out, 1);

        out->write(out, ',');
        json_write_string(out, "CanResume");
        out->write(out, ':');
        json_write_long(out, (1 << RM_RESUME) | (1 << RM_STEP_INTO));

        out->write(out, ',');
        json_write_string(out, "HasState");
        out->write(out, ':');
        json_write_boolean(out, 1);
    }

    out->write(out, '}');
}

static void write_context_state(OutputStream * out, Context * ctx) {
    char reason[128];

    assert(!ctx->exited);

    if (!ctx->intercepted) {
        write_stringz(out, "0");
        write_stringz(out, "null");
        write_stringz(out, "null");
        return;
    }

    /* Number: PC */
    json_write_ulong(out, get_regs_PC(ctx->regs));
    out->write(out, 0);

    /* String: Reason */
    if (ctx->event != 0) {
        assert(ctx->signal == SIGTRAP);
        snprintf(reason, sizeof(reason), "Event: %s", event_name(ctx->event));
    }
    else if (is_stopped_by_breakpoint(ctx)) {
        strcpy(reason, "Breakpoint");
    }
    else if (ctx->signal == SIGSTOP || ctx->signal == SIGTRAP) {
        strcpy(reason, "Suspended");
    }
    else if (signal_name(ctx->signal)) {
        snprintf(reason, sizeof(reason), "Signal %d %s", ctx->signal, signal_name(ctx->signal));
    }
    else {
        snprintf(reason, sizeof(reason), "Signal %d", ctx->signal);
    }
    json_write_string(out, reason);
    out->write(out, 0);

    /* Object: Aditional info */
    out->write(out, '{');
    json_write_string(out, "Event");
    out->write(out, ':');
    json_write_long(out, ctx->event);
    out->write(out, ',');
    json_write_string(out, "Signal");
    out->write(out, ':');
    json_write_long(out, ctx->signal);
    if (signal_name(ctx->signal)) {
        out->write(out, ',');
        json_write_string(out, "SignalName");
        out->write(out, ':');
        json_write_string(out, signal_name(ctx->signal));
    }
    out->write(out, '}');
    out->write(out, 0);
}

static void event_get_context(void * arg) {
    GetContextArgs * s = (GetContextArgs *)arg;
    Channel * c = s->c;
    Context * ctx = s->ctx;

    if (!is_stream_closed(c)) {
        int err = 0;

        write_stringz(&c->out, "R");
        write_stringz(&c->out, s->token);

        if (ctx->exited) err = ERR_ALREADY_EXITED;
        write_errno(&c->out, err);
    
        if (err == 0) {
            write_context(&c->out, ctx, s->parent != 0);
            c->out.write(&c->out, 0);
        }
        else {
            write_stringz(&c->out, "null");
        }

        c->out.write(&c->out, MARKER_EOM);
        c->out.flush(&c->out);
    }
    stream_unlock(c);
    context_unlock(ctx);
    loc_free(s);
}

static void command_get_context(char * token, Channel * c) {
    int err = 0;
    char id[256];
    Context * ctx = NULL;

    json_read_string(&c->inp, id, sizeof(id));
    if (c->inp.read(&c->inp) != 0) exception(ERR_JSON_SYNTAX);
    if (c->inp.read(&c->inp) != MARKER_EOM) exception(ERR_JSON_SYNTAX);

    ctx = id2ctx(id);
    
    if (ctx == NULL) err = ERR_INV_CONTEXT;
    else if (ctx->exited) err = ERR_ALREADY_EXITED;
    
    if (err) {
        write_stringz(&c->out, "R");
        write_stringz(&c->out, token);
        write_errno(&c->out, err);
        write_stringz(&c->out, "null");
        c->out.write(&c->out, MARKER_EOM);
    }
    else {
        /* Need to stop everything to access context properties.
         * In particular, proc FS access can fail when process is running.
         */
        GetContextArgs * s = loc_alloc_zero(sizeof(GetContextArgs));
        s->c = c;
        stream_lock(c);
        strcpy(s->token, token);
        s->ctx = ctx;
        context_lock(ctx);
        id2pid(id, &s->parent);
        post_safe_event(event_get_context, s);
    }
}

static void command_get_children(char * token, Channel * c) {
    char id[256];

    json_read_string(&c->inp, id, sizeof(id));
    if (c->inp.read(&c->inp) != 0) exception(ERR_JSON_SYNTAX);
    if (c->inp.read(&c->inp) != MARKER_EOM) exception(ERR_JSON_SYNTAX);

    write_stringz(&c->out, "R");
    write_stringz(&c->out, token);

    write_errno(&c->out, 0);

    c->out.write(&c->out, '[');
    if (id[0] == 0) {
        LINK * qp;
        int cnt = 0;
        for (qp = context_root.next; qp != &context_root; qp = qp->next) {
            Context * ctx = ctxl2ctxp(qp);
            if (ctx->exited) continue;
            if (ctx->parent != NULL) continue;
            if (cnt > 0) c->out.write(&c->out, ',');
            json_write_string(&c->out, container_id(ctx));
            cnt++;
        }
    }
    else if (id[0] == 'P') {
        LINK * qp;
        int cnt = 0;
        pid_t ppd = 0;
        pid_t pid = id2pid(id, &ppd);
        Context * parent = id2ctx(id);
        if (parent != NULL && parent->parent == NULL && ppd == 0) {
            if (!parent->exited) {
                if (cnt > 0) c->out.write(&c->out, ',');
                json_write_string(&c->out, thread_id(parent));
                cnt++;
            }
            for (qp = parent->children.next; qp != &parent->children; qp = qp->next) {
                Context * ctx = cldl2ctxp(qp);
                assert(!ctx->exited);
                assert(ctx->parent == parent);
                if (cnt > 0) c->out.write(&c->out, ',');
                json_write_string(&c->out,thread_id(ctx));
                cnt++;
            }
        }
    }
    c->out.write(&c->out, ']');
    c->out.write(&c->out, 0);

    c->out.write(&c->out, MARKER_EOM);
}

static void command_get_state(char * token, Channel * c) {
    char id[256];
    Context * ctx;
    int err = 0;

    json_read_string(&c->inp, id, sizeof(id));
    if (c->inp.read(&c->inp) != 0) exception(ERR_JSON_SYNTAX);
    if (c->inp.read(&c->inp) != MARKER_EOM) exception(ERR_JSON_SYNTAX);
    ctx = id2ctx(id);

    write_stringz(&c->out, "R");
    write_stringz(&c->out, token);

    if (ctx == NULL) err = ERR_INV_CONTEXT;
    else if (ctx->exited) err = ERR_ALREADY_EXITED;
    write_errno(&c->out, err);

    json_write_boolean(&c->out, ctx != NULL && ctx->intercepted);
    c->out.write(&c->out, 0);

    if (err) {
        write_stringz(&c->out, "0");
        write_stringz(&c->out, "null");
        write_stringz(&c->out, "null");
    }
    else {
        write_context_state(&c->out, ctx);
    }

    c->out.write(&c->out, MARKER_EOM);
}

static void send_simple_result(Channel * c, char * token, int err) {
    write_stringz(&c->out, "R");
    write_stringz(&c->out, token);
    write_errno(&c->out, err);
    c->out.write(&c->out, MARKER_EOM);
}

static void send_event_context_resumed(OutputStream * out, Context * ctx);

static void done_skip_breakpoint(SkipBreakpointInfo * s) {
    Channel * c = s->c;
    if (!is_stream_closed(c)) {
        send_simple_result(c, s->token, s->error);
        c->out.flush(&c->out);
    }
}

static void command_resume(char * token, Channel * c) {
    char id[256];
    long mode;
    long count;
    Context * ctx;
    int err = 0;

    json_read_string(&c->inp, id, sizeof(id));
    if (c->inp.read(&c->inp) != 0) exception(ERR_JSON_SYNTAX);
    mode = json_read_long(&c->inp);
    if (c->inp.read(&c->inp) != 0) exception(ERR_JSON_SYNTAX);
    count = json_read_long(&c->inp);
    if (c->inp.read(&c->inp) != 0) exception(ERR_JSON_SYNTAX);
    if (c->inp.read(&c->inp) != MARKER_EOM) exception(ERR_JSON_SYNTAX);
    ctx = id2ctx(id);
    assert(safe_event_list == NULL);

    if (ctx == NULL) {
        err = ERR_INV_CONTEXT;
    }
    else if (ctx->exited) {
        err = ERR_ALREADY_EXITED;
    }
    else if (!ctx->intercepted) {
        err = ERR_ALREADY_RUNNING;
    }
    else if (ctx->regs_error) {
        err = ctx->regs_error;
    }
    else if (count != 1) {
        err = EINVAL;
    }
    else if (mode == RM_RESUME || mode == RM_STEP_INTO) {
        SkipBreakpointInfo * sb = skip_breakpoint(ctx);
        send_event_context_resumed(&c->bcg->out, ctx);
        if (sb != NULL) {
            if (mode == RM_STEP_INTO) sb->pending_intercept = 1;
            sb->done = done_skip_breakpoint;
            sb->c = c;
            stream_lock(c);
            strcpy(sb->token, token);
            return;
        }
        if (mode == RM_STEP_INTO) {
            if (context_single_step(ctx) < 0) {
                err = errno;
            }
            else {
                ctx->pending_intercept = 1;
            }
        }
        else {
            if (context_continue(ctx) < 0) err = errno;
        }
    }
    else {
        err = EINVAL;
    }

    send_simple_result(c, token, err);
}

static void send_event_context_suspended(OutputStream * out, Context * ctx);

static void command_suspend(char * token, Channel * c) {
    char id[256];
    Context * ctx;
    int err = 0;

    json_read_string(&c->inp, id, sizeof(id));
    if (c->inp.read(&c->inp) != 0) exception(ERR_JSON_SYNTAX);
    if (c->inp.read(&c->inp) != MARKER_EOM) exception(ERR_JSON_SYNTAX);
    ctx = id2ctx(id);

    if (ctx == NULL) {
        err = ERR_INV_CONTEXT;
    }
    else if (ctx->exited) {
        err = ERR_ALREADY_EXITED;
    }
    else if (ctx->intercepted) {
        err = ERR_ALREADY_STOPPED;
    }
    else if (ctx->stopped) {
        send_event_context_suspended(&c->bcg->out, ctx);
    }
    else {
        ctx->pending_intercept = 1;
        if (context_stop(ctx) < 0) err = errno;
    }

    send_simple_result(c, token, err);
}

static void command_not_supported(char * token, Channel * c) {
    char id[256];

    json_read_string(&c->inp, id, sizeof(id));
    if (c->inp.read(&c->inp) != 0) exception(ERR_JSON_SYNTAX);
    if (c->inp.read(&c->inp) != MARKER_EOM) exception(ERR_JSON_SYNTAX);

    send_simple_result(c, token, ENOSYS);
}

static void send_event_context_added(OutputStream * out, Context * ctx) {
    write_stringz(out, "E");
    write_stringz(out, RUN_CONTROL);
    write_stringz(out, "contextAdded");

    /* <array of context data> */
    out->write(out, '[');
    if (ctx->parent == NULL) {
        write_context(out, ctx, 0);
        out->write(out, ',');
    }
    write_context(out, ctx, 1);
    out->write(out, ']');
    out->write(out, 0);

    out->write(out, MARKER_EOM);
}

static void send_event_context_changed(OutputStream * out, Context * ctx) {
    write_stringz(out, "E");
    write_stringz(out, RUN_CONTROL);
    write_stringz(out, "contextChanged");

    /* <array of context data> */
    out->write(out, '[');
    if (ctx->parent == NULL) {
        write_context(out, ctx, 0);
        out->write(out, ',');
    }
    write_context(out, ctx, 1);
    out->write(out, ']');
    out->write(out, 0);

    out->write(out, MARKER_EOM);
}

static void send_event_context_removed(OutputStream * out, Context * ctx) {
    write_stringz(out, "E");
    write_stringz(out, RUN_CONTROL);
    write_stringz(out, "contextRemoved");

    /* <array of context IDs> */
    out->write(out, '[');
    json_write_string(out, thread_id(ctx));
    if (ctx->parent == NULL && list_is_empty(&ctx->children)) {
        out->write(out, ',');
        json_write_string(out, container_id(ctx));
    }
    out->write(out, ']');
    out->write(out, 0);

    out->write(out, MARKER_EOM);
}

static void send_event_context_suspended(OutputStream * out, Context * ctx) {
    assert(!ctx->exited);
    assert(!ctx->intercepted);
    ctx->intercepted = 1;
    ctx->pending_intercept = 0;

    write_stringz(out, "E");
    write_stringz(out, RUN_CONTROL);
    write_stringz(out, "contextSuspended");

    /* String: Context ID */
    json_write_string(out, thread_id(ctx));
    out->write(out, 0);

    write_context_state(out, ctx);
    out->write(out, MARKER_EOM);
}

static void send_event_context_resumed(OutputStream * out, Context * ctx) {
    assert(ctx->intercepted);
    assert(!ctx->pending_intercept);
    ctx->intercepted = 0;

    write_stringz(out, "E");
    write_stringz(out, RUN_CONTROL);
    write_stringz(out, "contextResumed");

    /* String: Context ID */
    json_write_string(out, thread_id(ctx));
    out->write(out, 0);

    out->write(out, MARKER_EOM);
}

static void send_event_context_exception(OutputStream * out, Context * ctx) {
    char buf[128];

    write_stringz(out, "E");
    write_stringz(out, RUN_CONTROL);
    write_stringz(out, "contextException");

    /* String: Context ID */
    json_write_string(out, thread_id(ctx));
    out->write(out, 0);

    /* String: Human readable description of the exception */
    snprintf(buf, sizeof(buf), "Signal %d", ctx->signal);
    json_write_string(out, buf);
    out->write(out, 0);

    out->write(out, MARKER_EOM);
}

int is_all_stopped(void) {
    LINK * qp;
    for (qp = context_root.next; qp != &context_root; qp = qp->next) {
        Context * ctx = ctxl2ctxp(qp);
        if (ctx->exited || ctx->exiting) continue;
        if (!ctx->stopped) return 0;
    }
    return are_channels_suspended(suspend_group);
}

static void continue_temporary_stopped(void * arg) {
    LINK * qp;

    if ((int)arg != safe_event_generation) return;
    assert(safe_event_list == NULL);

    if (channels_get_message_count(suspend_group) > 0) {
        post_event(continue_temporary_stopped, (void *)safe_event_generation);
        return;
    }

    for (qp = context_root.next; qp != &context_root; qp = qp->next) {
        Context * ctx = ctxl2ctxp(qp);
        if (ctx->exited) continue;
        if (!ctx->stopped) continue;
        if (ctx->intercepted) continue;
        if (ctx->pending_step) continue;
        context_continue(ctx);
    }
}

static void run_safe_events(void * arg) {
    LINK * qp;

    if ((int)arg != safe_event_generation) return;
    assert(safe_event_list != NULL);
    assert(are_channels_suspended(suspend_group));

    for (qp = context_root.next; qp != &context_root; qp = qp->next) {
        Context * ctx = ctxl2ctxp(qp);
        if (ctx->exited || ctx->exiting) continue;
        if (!ctx->pending_step) {
            int error = 0;
            if (ctx->stopped) continue;
            if (context_stop(ctx) < 0) {
                error = errno;
#ifdef _WRS_KERNEL
                if (error == S_vxdbgLib_INVALID_CTX) {
                    /* Most often this means that context has exited,
                     * but exit event is not delivered yet.
                     * Not an error. */
                    error = 0;
                }
#endif                
            }
            if (error) {
                trace(LOG_ALWAYS, "error: can't temporary stop pid %d; error %d: %s",
                    ctx->pid, error, errno_to_str(error));
            }
        }
        if (!ctx->pending_safe_event) {
            ctx->pending_safe_event = 1;
            safe_event_pid_count++;
        }
        else if (ctx->pending_safe_event == STOP_ALL_MAX_CNT) {
            trace(LOG_ALWAYS, "error: can't temporary stop pid %d; error: timeout", ctx->pid);
            ctx->exiting = 1;
            ctx->pending_safe_event = 0;
            safe_event_pid_count--;
        }
        else {
            ctx->pending_safe_event++;
        }
    }

    if ((int)arg != safe_event_generation) return;

    while (safe_event_list) {
        SafeEvent * i = safe_event_list;
        if (safe_event_pid_count > 0) {
            post_event_with_delay(run_safe_events, (void *)++safe_event_generation, STOP_ALL_TIMEOUT);
            return;
        }
        assert(is_all_stopped());
        safe_event_list = i->next;
        // TODO: neeed to handle exceptions in "safe events"
        i->done(i->arg);
        loc_free(i);
        if ((int)arg != safe_event_generation) return;
    }

    channels_resume(suspend_group);
    /* Lazily continue execution of temporary stopped contexts */
    post_event(continue_temporary_stopped, (void *)safe_event_generation);
}

static void check_safe_events(Context * ctx) {
    assert(ctx->stopped || ctx->exited);
    assert(ctx->pending_safe_event);
    assert(safe_event_list != NULL);
    assert(safe_event_pid_count > 0);
    ctx->pending_safe_event = 0;
    safe_event_pid_count--;
    if (safe_event_pid_count == 0) {
        post_event(run_safe_events, (void *)++safe_event_generation);
    }
}

void post_safe_event(void (*done)(void *), void * arg) {
    SafeEvent * i = (SafeEvent *)loc_alloc(sizeof(SafeEvent));
    i->done = done;
    i->arg = arg;
    if (safe_event_list == NULL) {
        assert(safe_event_pid_count == 0);
        channels_suspend(suspend_group);
        post_event(run_safe_events, (void *)++safe_event_generation);
    }
    assert(are_channels_suspended(suspend_group));
    i->next = safe_event_list;
    safe_event_list = i;
}

static void event_context_created(Context * ctx, void * client_data) {
    TCFBroadcastGroup * bcg = client_data;
    assert(!ctx->exited);
    assert(!ctx->intercepted);
    assert(!ctx->stopped);
    send_event_context_added(&bcg->out, ctx);
    bcg->out.flush(&bcg->out);
}

static void event_context_changed(Context * ctx, void * client_data) {
    TCFBroadcastGroup * bcg = client_data;

    send_event_context_changed(&bcg->out, ctx);
    bcg->out.flush(&bcg->out);
}

static void event_context_stopped(Context * ctx, void * client_data) {
    TCFBroadcastGroup * bcg = client_data;

    assert(ctx->stopped);
    assert(!ctx->intercepted);
    assert(!ctx->exited);
    if (ctx->pending_safe_event) check_safe_events(ctx);
    if (is_stopped_by_breakpoint(ctx)) {
        if (evaluate_breakpoint_condition(ctx)) {
            ctx->pending_intercept = 1;
        }
        else {
            skip_breakpoint(ctx);
        }
    }
    else if (ctx->signal != SIGSTOP && ctx->signal != SIGTRAP) {
        send_event_context_exception(&bcg->out, ctx);
        ctx->pending_intercept = 1;
    }
    if (ctx->pending_intercept) {
        send_event_context_suspended(&bcg->out, ctx);
        bcg->out.flush(&bcg->out);
    }
    if (!ctx->intercepted && safe_event_list == NULL) {
        context_continue(ctx);
    }
}

static void event_context_started(Context * ctx, void * client_data) {
    TCFBroadcastGroup *bcg = client_data;

    assert(!ctx->stopped);
    assert(!ctx->intercepted);
    ctx->stopped_by_bp = 0;
    if (safe_event_list) {
        if (!ctx->pending_step) {
            context_stop(ctx);
        }
        if (!ctx->pending_safe_event) {
            ctx->pending_safe_event = 1;
            safe_event_pid_count++;
        }
    }
}

static void event_context_exited(Context * ctx, void * client_data) {
    TCFBroadcastGroup *bcg = client_data;

    assert(!ctx->stopped);
    assert(!ctx->intercepted);
    if (ctx->pending_safe_event) check_safe_events(ctx);
    send_event_context_removed(&bcg->out, ctx);
    bcg->out.flush(&bcg->out);
}

void ini_run_ctrl_service(Protocol * proto, TCFBroadcastGroup * bcg, TCFSuspendGroup * spg) {
    static ContextEventListener listener = {
        event_context_created,
        event_context_exited,
        event_context_stopped,
        event_context_started,
        event_context_changed
    };
    suspend_group = spg;
    add_context_event_listener(&listener, bcg);
    add_command_handler(proto, RUN_CONTROL, "getContext", command_get_context);
    add_command_handler(proto, RUN_CONTROL, "getChildren", command_get_children);
    add_command_handler(proto, RUN_CONTROL, "getState", command_get_state);
    add_command_handler(proto, RUN_CONTROL, "resume", command_resume);
    add_command_handler(proto, RUN_CONTROL, "suspend", command_suspend);
    add_command_handler(proto, RUN_CONTROL, "terminate", command_not_supported);
}

#endif
