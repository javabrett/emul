/*
gnomeinterface.c

(c) 1994-2011 Edward Swartz

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
 */
#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>

#include <bonobo.h>
#include <gnome.h>

#include "gnomecallbacks.h"
#include "gnomeinterface.h"
#include "gnomesupport.h"

#define GLADE_HOOKUP_OBJECT(component,widget,name) \
  g_object_set_data_full (G_OBJECT (component), name, \
    gtk_widget_ref (widget), (GDestroyNotify) gtk_widget_unref)

#define GLADE_HOOKUP_OBJECT_NO_REF(component,widget,name) \
  g_object_set_data (G_OBJECT (component), name, widget)

GtkWidget*
create_v9t9_window (void)
{
  GtkWidget *v9t9_window;
  GtkWidget *v9t9_drawing_area;

  v9t9_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  GTK_WIDGET_SET_FLAGS (v9t9_window, GTK_CAN_DEFAULT);
  gtk_widget_set_events (v9t9_window, GDK_KEY_RELEASE_MASK | GDK_ENTER_NOTIFY_MASK);
  gtk_window_set_title (GTK_WINDOW (v9t9_window), _("V9t9"));

  v9t9_drawing_area = gtk_drawing_area_new ();
  gtk_widget_show (v9t9_drawing_area);
  gtk_container_add (GTK_CONTAINER (v9t9_window), v9t9_drawing_area);
  GTK_WIDGET_SET_FLAGS (v9t9_drawing_area, GTK_CAN_FOCUS);
  GTK_WIDGET_SET_FLAGS (v9t9_drawing_area, GTK_CAN_DEFAULT);
  gtk_widget_set_events (v9t9_drawing_area, GDK_BUTTON_PRESS_MASK | GDK_BUTTON_RELEASE_MASK | GDK_KEY_RELEASE_MASK | GDK_ENTER_NOTIFY_MASK);

  g_signal_connect ((gpointer) v9t9_window, "configure_event",
                    G_CALLBACK (on_v9t9_window_configure_event),
                    NULL);
  g_signal_connect_after ((gpointer) v9t9_window, "enter_notify_event",
                          G_CALLBACK (on_v9t9_window_enter_notify_event),
                          NULL);
  g_signal_connect ((gpointer) v9t9_window, "destroy",
                    G_CALLBACK (on_v9t9_window_destroy),
                    NULL);
  g_signal_connect ((gpointer) v9t9_drawing_area, "configure_event",
                    G_CALLBACK (on_v9t9_draw_area_configure_event),
                    NULL);
  g_signal_connect ((gpointer) v9t9_drawing_area, "expose_event",
                    G_CALLBACK (on_v9t9_draw_area_expose_event),
                    NULL);
  g_signal_connect_after ((gpointer) v9t9_drawing_area, "key_press_event",
                          G_CALLBACK (on_v9t9_key_press_event),
                          NULL);
  g_signal_connect_after ((gpointer) v9t9_drawing_area, "key_release_event",
                          G_CALLBACK (on_v9t9_key_release_event),
                          NULL);
  g_signal_connect ((gpointer) v9t9_drawing_area, "size_request",
                    G_CALLBACK (on_v9t9_draw_area_size_request),
                    NULL);
  g_signal_connect ((gpointer) v9t9_drawing_area, "enter_notify_event",
                    G_CALLBACK (on_v9t9_draw_area_enter_notify_event),
                    NULL);
  g_signal_connect ((gpointer) v9t9_drawing_area, "button_press_event",
                    G_CALLBACK (on_v9t9_drawing_area_button_press_event),
                    NULL);
  g_signal_connect ((gpointer) v9t9_drawing_area, "realize",
                    G_CALLBACK (on_v9t9_draw_area_realize),
                    NULL);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (v9t9_window, v9t9_window, "v9t9_window");
  GLADE_HOOKUP_OBJECT (v9t9_window, v9t9_drawing_area, "v9t9_drawing_area");

  gtk_widget_grab_focus (v9t9_drawing_area);
  gtk_widget_grab_default (v9t9_drawing_area);
  return v9t9_window;
}

GtkWidget*
create_quit_dialog (void)
{
  GtkWidget *quit_dialog;
  GtkWidget *dialog_vbox1;
  GtkWidget *quit_message_text;
  GtkWidget *dialog_action_area1;
  GtkWidget *save_button;
  GtkWidget *die_button;
  GtkWidget *cancel_button;
  GtkAccelGroup *accel_group;

  accel_group = gtk_accel_group_new ();

  quit_dialog = gtk_dialog_new ();
  gtk_window_set_title (GTK_WINDOW (quit_dialog), _("Quit V9t9"));
  gtk_window_set_modal (GTK_WINDOW (quit_dialog), TRUE);

  dialog_vbox1 = GTK_DIALOG (quit_dialog)->vbox;
  gtk_widget_show (dialog_vbox1);

  quit_message_text = gtk_label_new (_("Save current session before quitting?\n"));
  gtk_widget_show (quit_message_text);
  gtk_box_pack_start (GTK_BOX (dialog_vbox1), quit_message_text, TRUE, TRUE, 0);
  gtk_label_set_justify (GTK_LABEL (quit_message_text), GTK_JUSTIFY_CENTER);
  gtk_misc_set_padding (GTK_MISC (quit_message_text), 16, 16);

  dialog_action_area1 = GTK_DIALOG (quit_dialog)->action_area;
  gtk_widget_show (dialog_action_area1);
  gtk_button_box_set_layout (GTK_BUTTON_BOX (dialog_action_area1), GTK_BUTTONBOX_END);

  save_button = gtk_button_new_with_mnemonic (_("_Yes, Save"));
  gtk_widget_show (save_button);
  gtk_dialog_add_action_widget (GTK_DIALOG (quit_dialog), save_button, 0);
  GTK_WIDGET_SET_FLAGS (save_button, GTK_CAN_DEFAULT);
  gtk_widget_add_accelerator (save_button, "clicked", accel_group,
                              GDK_y, 0,
                              GTK_ACCEL_VISIBLE);

  die_button = gtk_button_new_with_mnemonic (_("_No, Don't Save"));
  gtk_widget_show (die_button);
  gtk_dialog_add_action_widget (GTK_DIALOG (quit_dialog), die_button, 0);
  GTK_WIDGET_SET_FLAGS (die_button, GTK_CAN_DEFAULT);
  gtk_widget_add_accelerator (die_button, "clicked", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);
  gtk_widget_add_accelerator (die_button, "clicked", accel_group,
                              GDK_n, 0,
                              GTK_ACCEL_VISIBLE);

  cancel_button = gtk_button_new_with_mnemonic (_("_Don't Quit"));
  gtk_widget_show (cancel_button);
  gtk_dialog_add_action_widget (GTK_DIALOG (quit_dialog), cancel_button, 0);
  GTK_WIDGET_SET_FLAGS (cancel_button, GTK_CAN_DEFAULT);
  gtk_widget_add_accelerator (cancel_button, "clicked", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);
  gtk_widget_add_accelerator (cancel_button, "clicked", accel_group,
                              GDK_d, 0,
                              GTK_ACCEL_VISIBLE);

  g_signal_connect ((gpointer) save_button, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "Quit");
  g_signal_connect ((gpointer) die_button, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "Die");
  g_signal_connect ((gpointer) cancel_button, "clicked",
                    G_CALLBACK (on_v9t9_button_cancel),
                    quit_dialog);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (quit_dialog, quit_dialog, "quit_dialog");
  GLADE_HOOKUP_OBJECT_NO_REF (quit_dialog, dialog_vbox1, "dialog_vbox1");
  GLADE_HOOKUP_OBJECT (quit_dialog, quit_message_text, "quit_message_text");
  GLADE_HOOKUP_OBJECT_NO_REF (quit_dialog, dialog_action_area1, "dialog_action_area1");
  GLADE_HOOKUP_OBJECT (quit_dialog, save_button, "save_button");
  GLADE_HOOKUP_OBJECT (quit_dialog, die_button, "die_button");
  GLADE_HOOKUP_OBJECT (quit_dialog, cancel_button, "cancel_button");

  gtk_widget_grab_focus (save_button);
  gtk_widget_grab_default (save_button);
  gtk_window_add_accel_group (GTK_WINDOW (quit_dialog), accel_group);

  return quit_dialog;
}

GtkWidget*
create_command_log_font_selector (void)
{
  GtkWidget *command_log_font_selector;
  GtkWidget *ok_button1;
  GtkWidget *cancel_button1;
  GtkWidget *apply_button1;
  GtkWidget *fontselection1;
  GtkAccelGroup *accel_group;

  accel_group = gtk_accel_group_new ();

  command_log_font_selector = gtk_font_selection_dialog_new (_("Select Font"));
  gtk_container_set_border_width (GTK_CONTAINER (command_log_font_selector), 4);

  ok_button1 = GTK_FONT_SELECTION_DIALOG (command_log_font_selector)->ok_button;
  gtk_widget_show (ok_button1);
  GTK_WIDGET_SET_FLAGS (ok_button1, GTK_CAN_DEFAULT);
  gtk_widget_add_accelerator (ok_button1, "clicked", accel_group,
                              GDK_Return, 0,
                              GTK_ACCEL_VISIBLE);

  cancel_button1 = GTK_FONT_SELECTION_DIALOG (command_log_font_selector)->cancel_button;
  gtk_widget_show (cancel_button1);
  GTK_WIDGET_SET_FLAGS (cancel_button1, GTK_CAN_DEFAULT);
  gtk_widget_add_accelerator (cancel_button1, "clicked", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);

  apply_button1 = GTK_FONT_SELECTION_DIALOG (command_log_font_selector)->apply_button;
  gtk_widget_show (apply_button1);
  GTK_WIDGET_SET_FLAGS (apply_button1, GTK_CAN_DEFAULT);
  gtk_widget_add_accelerator (apply_button1, "clicked", accel_group,
                              GDK_space, 0,
                              GTK_ACCEL_VISIBLE);

  fontselection1 = GTK_FONT_SELECTION_DIALOG (command_log_font_selector)->fontsel;
  gtk_widget_show (fontselection1);
  gtk_container_set_border_width (GTK_CONTAINER (fontselection1), 4);

  g_signal_connect ((gpointer) ok_button1, "clicked",
                    G_CALLBACK (on_command_log_font_selector_ok_button1_clicked),
                    NULL);
  g_signal_connect ((gpointer) cancel_button1, "clicked",
                    G_CALLBACK (on_command_log_font_cancel1_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) apply_button1, "clicked",
                    G_CALLBACK (on_command_log_font_selector_apply_button1_clicked),
                    NULL);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (command_log_font_selector, command_log_font_selector, "command_log_font_selector");
  GLADE_HOOKUP_OBJECT_NO_REF (command_log_font_selector, ok_button1, "ok_button1");
  GLADE_HOOKUP_OBJECT_NO_REF (command_log_font_selector, cancel_button1, "cancel_button1");
  GLADE_HOOKUP_OBJECT_NO_REF (command_log_font_selector, apply_button1, "apply_button1");
  GLADE_HOOKUP_OBJECT_NO_REF (command_log_font_selector, fontselection1, "fontselection1");

  gtk_window_add_accel_group (GTK_WINDOW (command_log_font_selector), accel_group);

  return command_log_font_selector;
}

GtkWidget*
create_disks_dialog (void)
{
  GtkWidget *disks_dialog;
  GtkWidget *dialog_vbox3;
  GtkWidget *disk_options_notebook;
  GtkWidget *disk_info_table;
  GtkWidget *label18;
  GtkWidget *label19;
  GtkWidget *label20;
  GtkWidget *label21;
  GtkWidget *label22;
  GtkWidget *button25;
  GtkWidget *button27;
  GtkWidget *button28;
  GtkWidget *button29;
  GtkWidget *dsk1_entry;
  GtkWidget *dsk2_entry;
  GtkWidget *dsk3_entry;
  GtkWidget *dsk4_entry;
  GtkWidget *dsk5_entry;
  GtkWidget *button32;
  GtkWidget *label23;
  GtkWidget *fiad_options_vbox;
  GtkWidget *hbox6;
  GtkWidget *checkbutton2;
  GtkWidget *fiad_file_format_hbox;
  GtkWidget *radiobutton2;
  GSList *radiobutton2_group = NULL;
  GtkWidget *radiobutton3;
  GtkWidget *checkbutton5;
  GtkWidget *checkbutton6;
  GtkWidget *checkbutton7;
  GtkWidget *checkbutton8;
  GtkWidget *checkbutton9;
  GtkWidget *frame8;
  GtkWidget *table1;
  GtkWidget *emu_lone_dsr_entry;
  GtkWidget *emu_shared_dsr_entry;
  GtkWidget *label26;
  GtkWidget *label27;
  GtkWidget *button30;
  GtkWidget *button31;
  GtkWidget *label24;
  GtkWidget *doad_options_vbox;
  GtkWidget *frame9;
  GtkWidget *table2;
  GtkWidget *label29;
  GtkWidget *button34;
  GtkWidget *real_disk_dsr_entry;
  GtkWidget *label28;
  GtkWidget *label25;
  GtkWidget *hbox8;
  GtkWidget *checkbutton3;
  GtkWidget *checkbutton4;
  GtkWidget *dialog_action_area3;
  GtkWidget *close_button;
  GtkAccelGroup *accel_group;

  accel_group = gtk_accel_group_new ();

  disks_dialog = gtk_dialog_new ();
  gtk_window_set_title (GTK_WINDOW (disks_dialog), _("Disks"));

  dialog_vbox3 = GTK_DIALOG (disks_dialog)->vbox;
  gtk_widget_show (dialog_vbox3);

  disk_options_notebook = gtk_notebook_new ();
  gtk_widget_show (disk_options_notebook);
  gtk_box_pack_start (GTK_BOX (dialog_vbox3), disk_options_notebook, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (disk_options_notebook), 4);
  gtk_notebook_set_show_border (GTK_NOTEBOOK (disk_options_notebook), FALSE);

  disk_info_table = gtk_table_new (5, 3, FALSE);
  gtk_widget_show (disk_info_table);
  gtk_container_add (GTK_CONTAINER (disk_options_notebook), disk_info_table);
  gtk_container_set_border_width (GTK_CONTAINER (disk_info_table), 8);

  label18 = gtk_label_new_with_mnemonic (_("DSK_1"));
  gtk_widget_show (label18);
  gtk_table_attach (GTK_TABLE (disk_info_table), label18, 0, 1, 0, 1,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 16, 8);
  gtk_label_set_justify (GTK_LABEL (label18), GTK_JUSTIFY_CENTER);

  label19 = gtk_label_new_with_mnemonic (_("DSK_2"));
  gtk_widget_show (label19);
  gtk_table_attach (GTK_TABLE (disk_info_table), label19, 0, 1, 1, 2,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 16, 8);
  gtk_label_set_justify (GTK_LABEL (label19), GTK_JUSTIFY_CENTER);

  label20 = gtk_label_new_with_mnemonic (_("DSK_3"));
  gtk_widget_show (label20);
  gtk_table_attach (GTK_TABLE (disk_info_table), label20, 0, 1, 2, 3,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 16, 8);
  gtk_label_set_justify (GTK_LABEL (label20), GTK_JUSTIFY_CENTER);

  label21 = gtk_label_new_with_mnemonic (_("DSK_4"));
  gtk_widget_show (label21);
  gtk_table_attach (GTK_TABLE (disk_info_table), label21, 0, 1, 3, 4,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 16, 8);
  gtk_label_set_justify (GTK_LABEL (label21), GTK_JUSTIFY_CENTER);

  label22 = gtk_label_new_with_mnemonic (_("DSK_5"));
  gtk_widget_show (label22);
  gtk_table_attach (GTK_TABLE (disk_info_table), label22, 0, 1, 4, 5,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 16, 8);
  gtk_label_set_justify (GTK_LABEL (label22), GTK_JUSTIFY_CENTER);

  button25 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button25);
  gtk_table_attach (GTK_TABLE (disk_info_table), button25, 2, 3, 1, 2,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button25), 4);
  GTK_WIDGET_UNSET_FLAGS (button25, GTK_CAN_FOCUS);

  button27 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button27);
  gtk_table_attach (GTK_TABLE (disk_info_table), button27, 2, 3, 3, 4,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button27), 4);
  GTK_WIDGET_UNSET_FLAGS (button27, GTK_CAN_FOCUS);

  button28 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button28);
  gtk_table_attach (GTK_TABLE (disk_info_table), button28, 2, 3, 4, 5,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button28), 4);
  GTK_WIDGET_UNSET_FLAGS (button28, GTK_CAN_FOCUS);

  button29 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button29);
  gtk_table_attach (GTK_TABLE (disk_info_table), button29, 2, 3, 0, 1,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 16, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button29), 4);
  GTK_WIDGET_UNSET_FLAGS (button29, GTK_CAN_FOCUS);

  dsk1_entry = gtk_entry_new ();
  gtk_widget_show (dsk1_entry);
  gtk_table_attach (GTK_TABLE (disk_info_table), dsk1_entry, 1, 2, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  dsk2_entry = gtk_entry_new ();
  gtk_widget_show (dsk2_entry);
  gtk_table_attach (GTK_TABLE (disk_info_table), dsk2_entry, 1, 2, 1, 2,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  dsk3_entry = gtk_entry_new ();
  gtk_widget_show (dsk3_entry);
  gtk_table_attach (GTK_TABLE (disk_info_table), dsk3_entry, 1, 2, 2, 3,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  dsk4_entry = gtk_entry_new ();
  gtk_widget_show (dsk4_entry);
  gtk_table_attach (GTK_TABLE (disk_info_table), dsk4_entry, 1, 2, 3, 4,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  dsk5_entry = gtk_entry_new ();
  gtk_widget_show (dsk5_entry);
  gtk_table_attach (GTK_TABLE (disk_info_table), dsk5_entry, 1, 2, 4, 5,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  button32 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button32);
  gtk_table_attach (GTK_TABLE (disk_info_table), button32, 2, 3, 2, 3,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button32), 4);
  GTK_WIDGET_UNSET_FLAGS (button32, GTK_CAN_FOCUS);

  label23 = gtk_label_new (_("DSKx Mappings"));
  gtk_widget_show (label23);
  gtk_notebook_set_tab_label (GTK_NOTEBOOK (disk_options_notebook), gtk_notebook_get_nth_page (GTK_NOTEBOOK (disk_options_notebook), 0), label23);
  gtk_label_set_justify (GTK_LABEL (label23), GTK_JUSTIFY_CENTER);

  fiad_options_vbox = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (fiad_options_vbox);
  gtk_container_add (GTK_CONTAINER (disk_options_notebook), fiad_options_vbox);

  hbox6 = gtk_hbox_new (FALSE, 0);
  gtk_widget_show (hbox6);
  gtk_box_pack_start (GTK_BOX (fiad_options_vbox), hbox6, FALSE, FALSE, 0);

  checkbutton2 = gtk_check_button_new_with_mnemonic (_("_Convert file headers to:"));
  gtk_widget_show (checkbutton2);
  gtk_box_pack_start (GTK_BOX (hbox6), checkbutton2, FALSE, FALSE, 0);

  fiad_file_format_hbox = gtk_hbox_new (FALSE, 0);
  gtk_widget_show (fiad_file_format_hbox);
  gtk_box_pack_start (GTK_BOX (hbox6), fiad_file_format_hbox, TRUE, TRUE, 0);

  radiobutton2 = gtk_radio_button_new_with_mnemonic (NULL, _("_V9t9"));
  gtk_widget_show (radiobutton2);
  gtk_box_pack_start (GTK_BOX (fiad_file_format_hbox), radiobutton2, TRUE, FALSE, 0);
  gtk_radio_button_set_group (GTK_RADIO_BUTTON (radiobutton2), radiobutton2_group);
  radiobutton2_group = gtk_radio_button_get_group (GTK_RADIO_BUTTON (radiobutton2));

  radiobutton3 = gtk_radio_button_new_with_mnemonic (NULL, _("_TIFILES"));
  gtk_widget_show (radiobutton3);
  gtk_box_pack_start (GTK_BOX (fiad_file_format_hbox), radiobutton3, TRUE, FALSE, 0);
  gtk_radio_button_set_group (GTK_RADIO_BUTTON (radiobutton3), radiobutton2_group);
  radiobutton2_group = gtk_radio_button_get_group (GTK_RADIO_BUTTON (radiobutton3));

  checkbutton5 = gtk_check_button_new_with_mnemonic (_("Treat _unknown files as DIS/VAR 80"));
  gtk_widget_show (checkbutton5);
  gtk_box_pack_start (GTK_BOX (fiad_options_vbox), checkbutton5, FALSE, FALSE, 0);

  checkbutton6 = gtk_check_button_new_with_mnemonic (_("Allow \"DSKx.\" catalogs _longer than 127 filenames"));
  gtk_widget_show (checkbutton6);
  gtk_box_pack_start (GTK_BOX (fiad_options_vbox), checkbutton6, FALSE, FALSE, 0);

  checkbutton7 = gtk_check_button_new_with_mnemonic (_("_Repair damaged V9t9 files"));
  gtk_widget_show (checkbutton7);
  gtk_box_pack_start (GTK_BOX (fiad_options_vbox), checkbutton7, FALSE, FALSE, 0);

  checkbutton8 = gtk_check_button_new_with_mnemonic (_("_Fixup filenames for files created with V9t9 for DOS"));
  gtk_widget_show (checkbutton8);
  gtk_box_pack_start (GTK_BOX (fiad_options_vbox), checkbutton8, FALSE, FALSE, 0);

  checkbutton9 = gtk_check_button_new_with_mnemonic (_("_Generate filenames compatible with V9t9 for DOS"));
  gtk_widget_show (checkbutton9);
  gtk_box_pack_start (GTK_BOX (fiad_options_vbox), checkbutton9, FALSE, FALSE, 0);

  frame8 = gtk_frame_new (NULL);
  gtk_widget_show (frame8);
  gtk_box_pack_start (GTK_BOX (fiad_options_vbox), frame8, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (frame8), 4);

  table1 = gtk_table_new (2, 3, FALSE);
  gtk_widget_show (table1);
  gtk_container_add (GTK_CONTAINER (frame8), table1);

  emu_lone_dsr_entry = gtk_entry_new ();
  gtk_widget_show (emu_lone_dsr_entry);
  gtk_table_attach (GTK_TABLE (table1), emu_lone_dsr_entry, 1, 2, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  emu_shared_dsr_entry = gtk_entry_new ();
  gtk_widget_show (emu_shared_dsr_entry);
  gtk_table_attach (GTK_TABLE (table1), emu_shared_dsr_entry, 1, 2, 1, 2,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  label26 = gtk_label_new_with_mnemonic (_("_Lone"));
  gtk_widget_show (label26);
  gtk_table_attach (GTK_TABLE (table1), label26, 0, 1, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_label_set_justify (GTK_LABEL (label26), GTK_JUSTIFY_CENTER);

  label27 = gtk_label_new_with_mnemonic (_("_Shared"));
  gtk_widget_show (label27);
  gtk_table_attach (GTK_TABLE (table1), label27, 0, 1, 1, 2,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_label_set_justify (GTK_LABEL (label27), GTK_JUSTIFY_CENTER);

  button30 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button30);
  gtk_table_attach (GTK_TABLE (table1), button30, 2, 3, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button30), 4);

  button31 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button31);
  gtk_table_attach (GTK_TABLE (table1), button31, 2, 3, 1, 2,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button31), 4);

  label24 = gtk_label_new (_("DSR ROM Selection"));
  gtk_widget_show (label24);
  gtk_frame_set_label_widget (GTK_FRAME (frame8), label24);

  label24 = gtk_label_new (_("File Directory Options"));
  gtk_widget_show (label24);
  gtk_notebook_set_tab_label (GTK_NOTEBOOK (disk_options_notebook), gtk_notebook_get_nth_page (GTK_NOTEBOOK (disk_options_notebook), 1), label24);
  gtk_label_set_justify (GTK_LABEL (label24), GTK_JUSTIFY_CENTER);

  doad_options_vbox = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (doad_options_vbox);
  gtk_container_add (GTK_CONTAINER (disk_options_notebook), doad_options_vbox);

  frame9 = gtk_frame_new (NULL);
  gtk_widget_show (frame9);
  gtk_box_pack_start (GTK_BOX (doad_options_vbox), frame9, FALSE, FALSE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (frame9), 4);

  table2 = gtk_table_new (1, 3, FALSE);
  gtk_widget_show (table2);
  gtk_container_add (GTK_CONTAINER (frame9), table2);

  label29 = gtk_label_new_with_mnemonic (_("S_hared"));
  gtk_widget_show (label29);
  gtk_table_attach (GTK_TABLE (table2), label29, 0, 1, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_label_set_justify (GTK_LABEL (label29), GTK_JUSTIFY_CENTER);

  button34 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button34);
  gtk_table_attach (GTK_TABLE (table2), button34, 2, 3, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button34), 4);

  real_disk_dsr_entry = gtk_entry_new ();
  gtk_widget_show (real_disk_dsr_entry);
  gtk_table_attach (GTK_TABLE (table2), real_disk_dsr_entry, 1, 2, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  label28 = gtk_label_new (_("DSR ROM Selection"));
  gtk_widget_show (label28);
  gtk_frame_set_label_widget (GTK_FRAME (frame9), label28);

  label25 = gtk_label_new (_("Disk Image Options"));
  gtk_widget_show (label25);
  gtk_notebook_set_tab_label (GTK_NOTEBOOK (disk_options_notebook), gtk_notebook_get_nth_page (GTK_NOTEBOOK (disk_options_notebook), 2), label25);
  gtk_label_set_justify (GTK_LABEL (label25), GTK_JUSTIFY_CENTER);

  hbox8 = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (hbox8);
  gtk_box_pack_start (GTK_BOX (dialog_vbox3), hbox8, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (hbox8), 4);

  checkbutton3 = gtk_check_button_new_with_mnemonic (_("Use file directories (FIAD)"));
  gtk_widget_show (checkbutton3);
  gtk_box_pack_start (GTK_BOX (hbox8), checkbutton3, FALSE, FALSE, 0);

  checkbutton4 = gtk_check_button_new_with_mnemonic (_("Use disk images (DOAD)"));
  gtk_widget_show (checkbutton4);
  gtk_box_pack_start (GTK_BOX (hbox8), checkbutton4, FALSE, FALSE, 0);

  dialog_action_area3 = GTK_DIALOG (disks_dialog)->action_area;
  gtk_widget_show (dialog_action_area3);
  gtk_button_box_set_layout (GTK_BUTTON_BOX (dialog_action_area3), GTK_BUTTONBOX_END);

  close_button = gtk_button_new_with_mnemonic (_("Close"));
  gtk_widget_show (close_button);
  gtk_dialog_add_action_widget (GTK_DIALOG (disks_dialog), close_button, 0);
  gtk_container_set_border_width (GTK_CONTAINER (close_button), 4);
  gtk_widget_add_accelerator (close_button, "clicked", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);

  g_signal_connect ((gpointer) disk_info_table, "realize",
                    G_CALLBACK (on_disk_info_table_realize),
                    NULL);
  g_signal_connect ((gpointer) button25, "clicked",
                    G_CALLBACK (on_disk_choose_button_clicked),
                    (gpointer)2);
  g_signal_connect ((gpointer) button27, "clicked",
                    G_CALLBACK (on_disk_choose_button_clicked),
                    (gpointer)4);
  g_signal_connect ((gpointer) button28, "clicked",
                    G_CALLBACK (on_disk_choose_button_clicked),
                    (gpointer)5);
  g_signal_connect ((gpointer) button29, "clicked",
                    G_CALLBACK (on_disk_choose_button_clicked),
                    (gpointer)1);
  g_signal_connect ((gpointer) dsk1_entry, "activate",
                    G_CALLBACK (on_disk_combo_entry_activate),
                    (gpointer)1);
  g_signal_connect ((gpointer) dsk2_entry, "activate",
                    G_CALLBACK (on_disk_combo_entry_activate),
                    (gpointer)2);
  g_signal_connect ((gpointer) dsk3_entry, "activate",
                    G_CALLBACK (on_disk_combo_entry_activate),
                   (gpointer)3);
  g_signal_connect ((gpointer) dsk4_entry, "activate",
                    G_CALLBACK (on_disk_combo_entry_activate),
                    (gpointer)4);
  g_signal_connect ((gpointer) dsk5_entry, "activate",
                    G_CALLBACK (on_disk_combo_entry_activate),
                    (gpointer)5);
  g_signal_connect ((gpointer) button32, "clicked",
                    G_CALLBACK (on_disk_choose_button_clicked),
                    (gpointer)3);
  g_signal_connect ((gpointer) checkbutton2, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle_not),
                    (gpointer *)"KeepFileFormat");
  g_signal_connect ((gpointer) checkbutton2, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_inactive),
                    (gpointer *)"KeepFileFormat");
  g_signal_connect ((gpointer) checkbutton2, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_widget_enable),
                    (gpointer)fiad_file_format_hbox);
  g_signal_connect ((gpointer) checkbutton2, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_widget_enable),
                    (gpointer *)fiad_file_format_hbox);
  g_signal_connect ((gpointer) radiobutton2, "clicked",
                    G_CALLBACK (on_v9t9_togglebutton_clicked),
                    (gpointer *)"NewFileFormat V9t9\n");
  g_signal_connect ((gpointer) radiobutton2, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_active),
                     (gpointer *)"NewFileFormat");
  g_signal_connect ((gpointer) radiobutton3, "clicked",
                    G_CALLBACK (on_v9t9_togglebutton_clicked),
                    (gpointer *)"NewFileFormat TIFILES\n");
  g_signal_connect ((gpointer) radiobutton3, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_active),
                    (gpointer *)"NewFileFormat");
  g_signal_connect ((gpointer) checkbutton5, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                    (gpointer *)"UnknownFileIsText");
  g_signal_connect ((gpointer) checkbutton5, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_active),
                    (gpointer *)"UnknownFileIsText");
  g_signal_connect ((gpointer) checkbutton6, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                    (gpointer *)"AllowLongCatalogs");
  g_signal_connect ((gpointer) checkbutton6, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_active),
                    (gpointer *)"AllowLongCatalogs");
  g_signal_connect ((gpointer) checkbutton7, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                    (gpointer *)"RepairDamagedFiles");
  g_signal_connect ((gpointer) checkbutton7, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_active),
                    (gpointer *)"RepairDamagedFiles");
  g_signal_connect ((gpointer) checkbutton8, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                    (gpointer *)"FixupOldV9t9Filenames");
  g_signal_connect ((gpointer) checkbutton8, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_active),
                    (gpointer *)"FixupOldV9t9Filenames");
  g_signal_connect ((gpointer) checkbutton9, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                     (gpointer *)"GenerateOldV9t9Filenames");
  g_signal_connect ((gpointer) checkbutton9, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_active),
                     (gpointer *)"GenerateOldV9t9Filenames");
  g_signal_connect ((gpointer) emu_lone_dsr_entry, "activate",
                    G_CALLBACK (on_v9t9_rom_entry_activate),
                    (gpointer *)"EmuDiskDSRFilename");
  g_signal_connect ((gpointer) emu_lone_dsr_entry, "realize",
                    G_CALLBACK (on_v9t9_rom_entry_realize),
                    (gpointer *)"EmuDiskDSRFilename");
  g_signal_connect ((gpointer) emu_shared_dsr_entry, "activate",
                    G_CALLBACK (on_v9t9_rom_entry_activate),
                    (gpointer *)"EmuDiskSharedDSRFilename");
  g_signal_connect ((gpointer) emu_shared_dsr_entry, "realize",
                    G_CALLBACK (on_v9t9_rom_entry_realize),
                     (gpointer *)"EmuDiskSharedDSRFilename");
  g_signal_connect ((gpointer) button30, "clicked",
                    G_CALLBACK (on_v9t9_rom_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button31, "clicked",
                    G_CALLBACK (on_v9t9_rom_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button34, "clicked",
                    G_CALLBACK (on_v9t9_rom_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) real_disk_dsr_entry, "activate",
                    G_CALLBACK (on_v9t9_rom_entry_activate),
                     (gpointer *)"DiskDSRFilename");
  g_signal_connect ((gpointer) real_disk_dsr_entry, "realize",
                    G_CALLBACK (on_v9t9_rom_entry_realize),
                    (gpointer *)"DiskDSRFilename");
  g_signal_connect ((gpointer) checkbutton3, "toggled",
                    G_CALLBACK (on_emu_disk_cb_toggled),
                    disk_info_table);
  g_signal_connect ((gpointer) checkbutton3, "realize",
                    G_CALLBACK (on_emu_disk_cb_realize),
                    disk_info_table);
  g_signal_connect ((gpointer) checkbutton3, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_widget_enable),
                    fiad_options_vbox);
  g_signal_connect ((gpointer) checkbutton3, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_widget_enable),
                    fiad_options_vbox);
  g_signal_connect ((gpointer) checkbutton4, "toggled",
                    G_CALLBACK (on_real_disk_cb_toggled),
                    disk_info_table);
  g_signal_connect ((gpointer) checkbutton4, "realize",
                    G_CALLBACK (on_real_disk_cb_realize),
                    disk_info_table);
  g_signal_connect ((gpointer) checkbutton4, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_widget_enable),
                    doad_options_vbox);
  g_signal_connect ((gpointer) checkbutton4, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_widget_enable),
                    doad_options_vbox);
  g_signal_connect ((gpointer) close_button, "clicked",
                    G_CALLBACK (on_disk_dialog_close_button_clicked),
                    NULL);

  gtk_label_set_mnemonic_widget (GTK_LABEL (label18), dsk1_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label19), dsk2_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label20), dsk3_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label21), dsk4_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label22), dsk5_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label26), emu_lone_dsr_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label27), emu_shared_dsr_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label29), real_disk_dsr_entry);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (disks_dialog, disks_dialog, "disks_dialog");
  GLADE_HOOKUP_OBJECT_NO_REF (disks_dialog, dialog_vbox3, "dialog_vbox3");
  GLADE_HOOKUP_OBJECT (disks_dialog, disk_options_notebook, "disk_options_notebook");
  GLADE_HOOKUP_OBJECT (disks_dialog, disk_info_table, "disk_info_table");
  GLADE_HOOKUP_OBJECT (disks_dialog, label18, "label18");
  GLADE_HOOKUP_OBJECT (disks_dialog, label19, "label19");
  GLADE_HOOKUP_OBJECT (disks_dialog, label20, "label20");
  GLADE_HOOKUP_OBJECT (disks_dialog, label21, "label21");
  GLADE_HOOKUP_OBJECT (disks_dialog, label22, "label22");
  GLADE_HOOKUP_OBJECT (disks_dialog, button25, "button25");
  GLADE_HOOKUP_OBJECT (disks_dialog, button27, "button27");
  GLADE_HOOKUP_OBJECT (disks_dialog, button28, "button28");
  GLADE_HOOKUP_OBJECT (disks_dialog, button29, "button29");
  GLADE_HOOKUP_OBJECT (disks_dialog, dsk1_entry, "dsk1_entry");
  GLADE_HOOKUP_OBJECT (disks_dialog, dsk2_entry, "dsk2_entry");
  GLADE_HOOKUP_OBJECT (disks_dialog, dsk3_entry, "dsk3_entry");
  GLADE_HOOKUP_OBJECT (disks_dialog, dsk4_entry, "dsk4_entry");
  GLADE_HOOKUP_OBJECT (disks_dialog, dsk5_entry, "dsk5_entry");
  GLADE_HOOKUP_OBJECT (disks_dialog, button32, "button32");
  GLADE_HOOKUP_OBJECT (disks_dialog, label23, "label23");
  GLADE_HOOKUP_OBJECT (disks_dialog, fiad_options_vbox, "fiad_options_vbox");
  GLADE_HOOKUP_OBJECT (disks_dialog, hbox6, "hbox6");
  GLADE_HOOKUP_OBJECT (disks_dialog, checkbutton2, "checkbutton2");
  GLADE_HOOKUP_OBJECT (disks_dialog, fiad_file_format_hbox, "fiad_file_format_hbox");
  GLADE_HOOKUP_OBJECT (disks_dialog, radiobutton2, "radiobutton2");
  GLADE_HOOKUP_OBJECT (disks_dialog, radiobutton3, "radiobutton3");
  GLADE_HOOKUP_OBJECT (disks_dialog, checkbutton5, "checkbutton5");
  GLADE_HOOKUP_OBJECT (disks_dialog, checkbutton6, "checkbutton6");
  GLADE_HOOKUP_OBJECT (disks_dialog, checkbutton7, "checkbutton7");
  GLADE_HOOKUP_OBJECT (disks_dialog, checkbutton8, "checkbutton8");
  GLADE_HOOKUP_OBJECT (disks_dialog, checkbutton9, "checkbutton9");
  GLADE_HOOKUP_OBJECT (disks_dialog, frame8, "frame8");
  GLADE_HOOKUP_OBJECT (disks_dialog, table1, "table1");
  GLADE_HOOKUP_OBJECT (disks_dialog, emu_lone_dsr_entry, "emu_lone_dsr_entry");
  GLADE_HOOKUP_OBJECT (disks_dialog, emu_shared_dsr_entry, "emu_shared_dsr_entry");
  GLADE_HOOKUP_OBJECT (disks_dialog, label26, "label26");
  GLADE_HOOKUP_OBJECT (disks_dialog, label27, "label27");
  GLADE_HOOKUP_OBJECT (disks_dialog, button30, "button30");
  GLADE_HOOKUP_OBJECT (disks_dialog, button31, "button31");
  GLADE_HOOKUP_OBJECT (disks_dialog, label24, "label24");
  GLADE_HOOKUP_OBJECT (disks_dialog, label24, "label24");
  GLADE_HOOKUP_OBJECT (disks_dialog, doad_options_vbox, "doad_options_vbox");
  GLADE_HOOKUP_OBJECT (disks_dialog, frame9, "frame9");
  GLADE_HOOKUP_OBJECT (disks_dialog, table2, "table2");
  GLADE_HOOKUP_OBJECT (disks_dialog, label29, "label29");
  GLADE_HOOKUP_OBJECT (disks_dialog, button34, "button34");
  GLADE_HOOKUP_OBJECT (disks_dialog, real_disk_dsr_entry, "real_disk_dsr_entry");
  GLADE_HOOKUP_OBJECT (disks_dialog, label28, "label28");
  GLADE_HOOKUP_OBJECT (disks_dialog, label25, "label25");
  GLADE_HOOKUP_OBJECT (disks_dialog, hbox8, "hbox8");
  GLADE_HOOKUP_OBJECT (disks_dialog, checkbutton3, "checkbutton3");
  GLADE_HOOKUP_OBJECT (disks_dialog, checkbutton4, "checkbutton4");
  GLADE_HOOKUP_OBJECT_NO_REF (disks_dialog, dialog_action_area3, "dialog_action_area3");
  GLADE_HOOKUP_OBJECT (disks_dialog, close_button, "close_button");

  gtk_window_add_accel_group (GTK_WINDOW (disks_dialog), accel_group);

  return disks_dialog;
}

GtkWidget*
create_modules_dialog (void)
{
  GtkWidget *modules_dialog;
  GtkWidget *dialog_vbox2;
  GtkWidget *scrolledwindow2;
  GtkWidget *module_clist;
  GtkWidget *dialog_action_area2;
  GtkWidget *vbox2;
  GtkWidget *hbox2;
  GtkWidget *show_commands_cb;
  GtkWidget *reset_computer_cb;
  GtkWidget *hbox1;
  GtkWidget *unload_current_button;
  GtkWidget *vseparator2;
  GtkWidget *close_button;
  GtkWidget *load_button;
  GtkWidget *refresh_button;
  GtkAccelGroup *accel_group;
  GtkTooltips *tooltips;

  tooltips = gtk_tooltips_new ();

  accel_group = gtk_accel_group_new ();

  modules_dialog = gtk_dialog_new ();
  gtk_window_set_title (GTK_WINDOW (modules_dialog), _("Modules"));
  gtk_window_set_default_size (GTK_WINDOW (modules_dialog), 400, 400);

  dialog_vbox2 = GTK_DIALOG (modules_dialog)->vbox;
  gtk_widget_show (dialog_vbox2);

  scrolledwindow2 = gtk_scrolled_window_new (NULL, NULL);
  gtk_widget_show (scrolledwindow2);
  gtk_box_pack_start (GTK_BOX (dialog_vbox2), scrolledwindow2, TRUE, TRUE, 0);
  GTK_WIDGET_UNSET_FLAGS (scrolledwindow2, GTK_CAN_FOCUS);
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (scrolledwindow2), GTK_POLICY_AUTOMATIC, GTK_POLICY_ALWAYS);
  gtk_scrolled_window_set_shadow_type (GTK_SCROLLED_WINDOW (scrolledwindow2), GTK_SHADOW_IN);

  module_clist = gtk_tree_view_new ();
  gtk_widget_show (module_clist);
  gtk_container_add (GTK_CONTAINER (scrolledwindow2), module_clist);
  GTK_WIDGET_SET_FLAGS (module_clist, GTK_CAN_DEFAULT);

  dialog_action_area2 = GTK_DIALOG (modules_dialog)->action_area;
  gtk_widget_show (dialog_action_area2);
  gtk_button_box_set_layout (GTK_BUTTON_BOX (dialog_action_area2), GTK_BUTTONBOX_END);

  vbox2 = gtk_vbox_new (TRUE, 0);
  gtk_widget_show (vbox2);
  gtk_container_add (GTK_CONTAINER (dialog_action_area2), vbox2);

  hbox2 = gtk_hbox_new (FALSE, 0);
  gtk_widget_show (hbox2);
  gtk_box_pack_start (GTK_BOX (vbox2), hbox2, FALSE, FALSE, 0);

  show_commands_cb = gtk_check_button_new_with_mnemonic (_("Show _setup commands"));
  gtk_widget_show (show_commands_cb);
  gtk_box_pack_start (GTK_BOX (hbox2), show_commands_cb, TRUE, TRUE, 0);
  gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (show_commands_cb), TRUE);

  reset_computer_cb = gtk_check_button_new_with_mnemonic (_("_Reset computer after load"));
  gtk_widget_show (reset_computer_cb);
  gtk_box_pack_start (GTK_BOX (hbox2), reset_computer_cb, TRUE, TRUE, 0);
  gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (reset_computer_cb), TRUE);

  hbox1 = gtk_hbox_new (TRUE, 4);
  gtk_widget_show (hbox1);
  gtk_box_pack_start (GTK_BOX (vbox2), hbox1, FALSE, FALSE, 0);

  unload_current_button = gtk_button_new_with_mnemonic (_("_Unload"));
  gtk_widget_show (unload_current_button);
  gtk_box_pack_start (GTK_BOX (hbox1), unload_current_button, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (unload_current_button), 4);
  gtk_tooltips_set_tip (tooltips, unload_current_button, _("Pull out the module while V9t9 is running"), NULL);
  gtk_widget_add_accelerator (unload_current_button, "clicked", accel_group,
                              GDK_u, GDK_MOD1_MASK,
                              GTK_ACCEL_VISIBLE);

  vseparator2 = gtk_vseparator_new ();
  gtk_widget_show (vseparator2);
  gtk_box_pack_start (GTK_BOX (hbox1), vseparator2, TRUE, TRUE, 0);

  close_button = gtk_button_new_with_mnemonic (_("Close"));
  gtk_widget_show (close_button);
  gtk_box_pack_end (GTK_BOX (hbox1), close_button, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (close_button), 4);
  gtk_widget_add_accelerator (close_button, "clicked", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);

  load_button = gtk_button_new_with_mnemonic (_("_Load"));
  gtk_widget_show (load_button);
  gtk_box_pack_end (GTK_BOX (hbox1), load_button, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (load_button), 4);
  gtk_tooltips_set_tip (tooltips, load_button, _("Insert module"), NULL);
  gtk_widget_add_accelerator (load_button, "clicked", accel_group,
                              GDK_l, GDK_MOD1_MASK,
                              GTK_ACCEL_VISIBLE);
  gtk_widget_add_accelerator (load_button, "clicked", accel_group,
                              GDK_Return, 0,
                              GTK_ACCEL_VISIBLE);

  refresh_button = gtk_button_new_with_mnemonic (_("Re_fresh"));
  gtk_widget_show (refresh_button);
  gtk_box_pack_end (GTK_BOX (hbox1), refresh_button, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (refresh_button), 4);
  gtk_tooltips_set_tip (tooltips, refresh_button, _("Reload module database from \"modules.inf\""), NULL);
  gtk_widget_add_accelerator (refresh_button, "clicked", accel_group,
                              GDK_f, GDK_MOD1_MASK,
                              GTK_ACCEL_VISIBLE);

  g_signal_connect ((gpointer) module_clist, "click_column",
                    G_CALLBACK (on_module_clist_click_column),
                    NULL);
  g_signal_connect ((gpointer) module_clist, "realize",
                    G_CALLBACK (on_module_clist_realize),
                    NULL);
  g_signal_connect ((gpointer) module_clist, "event",
                    G_CALLBACK (on_module_clist_event),
                    NULL);
  g_signal_connect_after ((gpointer) module_clist, "key_press_event",
                          G_CALLBACK (on_clist_key_press_event),
                          NULL);
  g_signal_connect_after ((gpointer) module_clist, "key_press_event",
                          G_CALLBACK (on_clist_key_press_event),
                          NULL);
  g_signal_connect ((gpointer) module_clist, "key_release_event",
                    G_CALLBACK (on_clist_key_release_event),
                    NULL);
  g_signal_connect ((gpointer) show_commands_cb, "toggled",
                    G_CALLBACK (on_show_commands_cb_toggled),
                   	   module_clist);
  g_signal_connect ((gpointer) reset_computer_cb, "toggled",
                    G_CALLBACK (on_reset_computer_cb_toggled),
                    NULL);
  g_signal_connect ((gpointer) unload_current_button, "clicked",
                    G_CALLBACK (on_unload_current_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) close_button, "clicked",
                    G_CALLBACK (on_module_clist_close_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) load_button, "clicked",
                    G_CALLBACK (on_module_clist_load_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) refresh_button, "clicked",
                    G_CALLBACK (on_modules_refresh_button_clicked),
                    NULL);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (modules_dialog, modules_dialog, "modules_dialog");
  GLADE_HOOKUP_OBJECT_NO_REF (modules_dialog, dialog_vbox2, "dialog_vbox2");
  GLADE_HOOKUP_OBJECT (modules_dialog, scrolledwindow2, "scrolledwindow2");
  GLADE_HOOKUP_OBJECT (modules_dialog, module_clist, "module_clist");
  GLADE_HOOKUP_OBJECT_NO_REF (modules_dialog, dialog_action_area2, "dialog_action_area2");
  GLADE_HOOKUP_OBJECT (modules_dialog, vbox2, "vbox2");
  GLADE_HOOKUP_OBJECT (modules_dialog, hbox2, "hbox2");
  GLADE_HOOKUP_OBJECT (modules_dialog, show_commands_cb, "show_commands_cb");
  GLADE_HOOKUP_OBJECT (modules_dialog, reset_computer_cb, "reset_computer_cb");
  GLADE_HOOKUP_OBJECT (modules_dialog, hbox1, "hbox1");
  GLADE_HOOKUP_OBJECT (modules_dialog, unload_current_button, "unload_current_button");
  GLADE_HOOKUP_OBJECT (modules_dialog, vseparator2, "vseparator2");
  GLADE_HOOKUP_OBJECT (modules_dialog, close_button, "close_button");
  GLADE_HOOKUP_OBJECT (modules_dialog, load_button, "load_button");
  GLADE_HOOKUP_OBJECT (modules_dialog, refresh_button, "refresh_button");
  GLADE_HOOKUP_OBJECT_NO_REF (modules_dialog, tooltips, "tooltips");

  gtk_widget_grab_focus (module_clist);
  gtk_window_add_accel_group (GTK_WINDOW (modules_dialog), accel_group);

  return modules_dialog;
}

GtkWidget*
create_debugger_window (void)
{
  GtkWidget *debugger_window;
  GtkWidget *vbox7;
  GtkWidget *hbox4;
  GtkWidget *registers_frame;
  GtkWidget *vbox8;
  GtkWidget *debugger_registers_table;
  GtkWidget *wp_edit_hbox;
  GtkWidget *label6;
  GtkWidget *debugger_wp_entry;
  GtkWidget *pc_edit_hbox;
  GtkWidget *label7;
  GtkWidget *debugger_pc_entry;
  GtkWidget *st_edit_hbox;
  GtkWidget *label8;
  GtkWidget *debugger_st_entry;
  GtkWidget *label30;
  GtkWidget *hpaned1;
  GtkWidget *instructions_frame;
  GtkWidget *instructions_scrolledwindow;
  GtkWidget *debugger_instruction_box;
  GtkWidget *label31;
  GtkWidget *vpaned1;
  GtkWidget *cpu_1_memory_frame;
  GtkWidget *scrolledwindow15;
  GtkWidget *viewport1;
  GtkWidget *text18;
  GtkWidget *label32;
  GtkWidget *vpaned2;
  GtkWidget *cpu_2_memory_frame;
  GtkWidget *scrolledwindow17;
  GtkWidget *viewport2;
  GtkWidget *text19;
  GtkWidget *label33;
  GtkWidget *vpaned3;
  GtkWidget *video_memory_frame;
  GtkWidget *scrolledwindow19;
  GtkWidget *viewport3;
  GtkWidget *text20;
  GtkWidget *label34;
  GtkWidget *vpaned4;
  GtkWidget *graphics_memory_frame;
  GtkWidget *scrolledwindow21;
  GtkWidget *viewport4;
  GtkWidget *text21;
  GtkWidget *label35;
  GtkWidget *speech_memory_frame;
  GtkWidget *scrolledwindow23;
  GtkWidget *viewport5;
  GtkWidget *text22;
  GtkWidget *label36;
  GtkWidget *hbox15;
  GtkWidget *vbox15;
  GtkWidget *hbox10;
  GtkWidget *button44;
  GtkWidget *button41;
  GtkWidget *button96;
  GtkWidget *button97;
  GtkWidget *hbox14;
  GtkWidget *button89;
  GtkWidget *button88;
  GtkWidget *button40;
  GtkWidget *label48;
  GtkWidget *table5;
  GtkWidget *label43;
  GtkObject *spinbutton1_adj;
  GtkWidget *spinbutton1;
  GtkWidget *debugger_status_bar;
  GtkAccelGroup *accel_group;
  GtkTooltips *tooltips;

  tooltips = gtk_tooltips_new ();

  accel_group = gtk_accel_group_new ();

  debugger_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  gtk_window_set_title (GTK_WINDOW (debugger_window), _("V9t9 Debugger"));
  gtk_window_set_default_size (GTK_WINDOW (debugger_window), 600, -1);

  vbox7 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox7);
  gtk_container_add (GTK_CONTAINER (debugger_window), vbox7);

  hbox4 = gtk_hbox_new (FALSE, 0);
  gtk_widget_show (hbox4);
  gtk_box_pack_start (GTK_BOX (vbox7), hbox4, TRUE, TRUE, 0);

  registers_frame = gtk_frame_new (NULL);
  gtk_widget_show (registers_frame);
  gtk_box_pack_start (GTK_BOX (hbox4), registers_frame, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (registers_frame), 2);

  vbox8 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox8);
  gtk_container_add (GTK_CONTAINER (registers_frame), vbox8);

  debugger_registers_table = gtk_table_new (1, 1, TRUE);
  gtk_widget_show (debugger_registers_table);
  gtk_box_pack_start (GTK_BOX (vbox8), debugger_registers_table, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (debugger_registers_table), 2);
  gtk_table_set_col_spacings (GTK_TABLE (debugger_registers_table), 2);

  wp_edit_hbox = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (wp_edit_hbox);
  gtk_box_pack_start (GTK_BOX (vbox8), wp_edit_hbox, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (wp_edit_hbox), 2);

  label6 = gtk_label_new (_("WP"));
  gtk_widget_show (label6);
  gtk_box_pack_start (GTK_BOX (wp_edit_hbox), label6, FALSE, FALSE, 4);
  gtk_label_set_justify (GTK_LABEL (label6), GTK_JUSTIFY_CENTER);

  debugger_wp_entry = gtk_entry_new ();
  gtk_widget_show (debugger_wp_entry);
  gtk_box_pack_start (GTK_BOX (wp_edit_hbox), debugger_wp_entry, TRUE, TRUE, 2);

  pc_edit_hbox = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (pc_edit_hbox);
  gtk_box_pack_start (GTK_BOX (vbox8), pc_edit_hbox, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (pc_edit_hbox), 2);

  label7 = gtk_label_new (_("PC"));
  gtk_widget_show (label7);
  gtk_box_pack_start (GTK_BOX (pc_edit_hbox), label7, FALSE, FALSE, 4);
  gtk_label_set_justify (GTK_LABEL (label7), GTK_JUSTIFY_CENTER);

  debugger_pc_entry = gtk_entry_new ();
  gtk_widget_show (debugger_pc_entry);
  gtk_box_pack_start (GTK_BOX (pc_edit_hbox), debugger_pc_entry, TRUE, TRUE, 2);

  st_edit_hbox = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (st_edit_hbox);
  gtk_box_pack_start (GTK_BOX (vbox8), st_edit_hbox, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (st_edit_hbox), 2);

  label8 = gtk_label_new (_("Status"));
  gtk_widget_show (label8);
  gtk_box_pack_start (GTK_BOX (st_edit_hbox), label8, FALSE, FALSE, 0);
  gtk_label_set_justify (GTK_LABEL (label8), GTK_JUSTIFY_CENTER);

  debugger_st_entry = gtk_entry_new ();
  gtk_widget_show (debugger_st_entry);
  gtk_box_pack_start (GTK_BOX (st_edit_hbox), debugger_st_entry, TRUE, TRUE, 2);

  label30 = gtk_label_new (_("Registers"));
  gtk_widget_show (label30);
  gtk_frame_set_label_widget (GTK_FRAME (registers_frame), label30);

  hpaned1 = gtk_hpaned_new ();
  gtk_widget_show (hpaned1);
  gtk_box_pack_start (GTK_BOX (hbox4), hpaned1, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (hpaned1), 1);
  GTK_WIDGET_UNSET_FLAGS (hpaned1, GTK_CAN_FOCUS);
  gtk_paned_set_position (GTK_PANED (hpaned1), 0);

  instructions_frame = gtk_frame_new (NULL);
  gtk_widget_show (instructions_frame);
  gtk_paned_pack1 (GTK_PANED (hpaned1), instructions_frame, TRUE, FALSE);

  instructions_scrolledwindow = gtk_scrolled_window_new (NULL, NULL);
  gtk_widget_show (instructions_scrolledwindow);
  gtk_container_add (GTK_CONTAINER (instructions_frame), instructions_scrolledwindow);
  GTK_WIDGET_UNSET_FLAGS (instructions_scrolledwindow, GTK_CAN_FOCUS);
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (instructions_scrolledwindow), GTK_POLICY_NEVER, GTK_POLICY_ALWAYS);
  gtk_scrolled_window_set_shadow_type (GTK_SCROLLED_WINDOW (instructions_scrolledwindow), GTK_SHADOW_IN);

  debugger_instruction_box = gtk_text_view_new ();
  gtk_widget_show (debugger_instruction_box);
  gtk_container_add (GTK_CONTAINER (instructions_scrolledwindow), debugger_instruction_box);
  GTK_WIDGET_UNSET_FLAGS (debugger_instruction_box, GTK_CAN_FOCUS);
  gtk_text_view_set_editable (GTK_TEXT_VIEW (debugger_instruction_box), FALSE);
  gtk_text_view_set_wrap_mode (GTK_TEXT_VIEW (debugger_instruction_box), GTK_WRAP_WORD);

  label31 = gtk_label_new (_("Instructions"));
  gtk_widget_show (label31);
  gtk_frame_set_label_widget (GTK_FRAME (instructions_frame), label31);

  vpaned1 = gtk_vpaned_new ();
  gtk_widget_show (vpaned1);
  gtk_paned_pack2 (GTK_PANED (hpaned1), vpaned1, TRUE, TRUE);
  GTK_WIDGET_UNSET_FLAGS (vpaned1, GTK_CAN_FOCUS);
  gtk_paned_set_position (GTK_PANED (vpaned1), 0);

  cpu_1_memory_frame = gtk_frame_new (NULL);
  gtk_widget_show (cpu_1_memory_frame);
  gtk_paned_pack1 (GTK_PANED (vpaned1), cpu_1_memory_frame, TRUE, FALSE);
  gtk_container_set_border_width (GTK_CONTAINER (cpu_1_memory_frame), 2);

  scrolledwindow15 = gtk_scrolled_window_new (NULL, NULL);
  gtk_widget_show (scrolledwindow15);
  gtk_container_add (GTK_CONTAINER (cpu_1_memory_frame), scrolledwindow15);
  GTK_WIDGET_UNSET_FLAGS (scrolledwindow15, GTK_CAN_FOCUS);
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (scrolledwindow15), GTK_POLICY_NEVER, GTK_POLICY_NEVER);

  viewport1 = gtk_viewport_new (NULL, NULL);
  gtk_widget_show (viewport1);
  gtk_container_add (GTK_CONTAINER (scrolledwindow15), viewport1);

  text18 = gtk_text_view_new ();
  gtk_widget_show (text18);
  gtk_container_add (GTK_CONTAINER (viewport1), text18);
  gtk_text_view_set_editable (GTK_TEXT_VIEW (text18), FALSE);
  gtk_text_view_set_wrap_mode (GTK_TEXT_VIEW (text18), GTK_WRAP_WORD);

  label32 = gtk_label_new (_("CPU Memory 1"));
  gtk_widget_show (label32);
  gtk_frame_set_label_widget (GTK_FRAME (cpu_1_memory_frame), label32);

  vpaned2 = gtk_vpaned_new ();
  gtk_widget_show (vpaned2);
  gtk_paned_pack2 (GTK_PANED (vpaned1), vpaned2, TRUE, TRUE);
  GTK_WIDGET_UNSET_FLAGS (vpaned2, GTK_CAN_FOCUS);
  gtk_paned_set_position (GTK_PANED (vpaned2), 1);

  cpu_2_memory_frame = gtk_frame_new (NULL);
  gtk_widget_show (cpu_2_memory_frame);
  gtk_paned_pack1 (GTK_PANED (vpaned2), cpu_2_memory_frame, TRUE, FALSE);
  gtk_container_set_border_width (GTK_CONTAINER (cpu_2_memory_frame), 2);

  scrolledwindow17 = gtk_scrolled_window_new (NULL, NULL);
  gtk_widget_show (scrolledwindow17);
  gtk_container_add (GTK_CONTAINER (cpu_2_memory_frame), scrolledwindow17);
  GTK_WIDGET_UNSET_FLAGS (scrolledwindow17, GTK_CAN_FOCUS);
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (scrolledwindow17), GTK_POLICY_NEVER, GTK_POLICY_NEVER);

  viewport2 = gtk_viewport_new (NULL, NULL);
  gtk_widget_show (viewport2);
  gtk_container_add (GTK_CONTAINER (scrolledwindow17), viewport2);

  text19 = gtk_text_view_new ();
  gtk_widget_show (text19);
  gtk_container_add (GTK_CONTAINER (viewport2), text19);
  gtk_text_view_set_editable (GTK_TEXT_VIEW (text19), FALSE);
  gtk_text_view_set_wrap_mode (GTK_TEXT_VIEW (text19), GTK_WRAP_WORD);

  label33 = gtk_label_new (_("CPU Memory 2"));
  gtk_widget_show (label33);
  gtk_frame_set_label_widget (GTK_FRAME (cpu_2_memory_frame), label33);

  vpaned3 = gtk_vpaned_new ();
  gtk_widget_show (vpaned3);
  gtk_paned_pack2 (GTK_PANED (vpaned2), vpaned3, TRUE, TRUE);
  GTK_WIDGET_UNSET_FLAGS (vpaned3, GTK_CAN_FOCUS);
  gtk_paned_set_position (GTK_PANED (vpaned3), 0);

  video_memory_frame = gtk_frame_new (NULL);
  gtk_widget_show (video_memory_frame);
  gtk_paned_pack1 (GTK_PANED (vpaned3), video_memory_frame, TRUE, FALSE);
  gtk_container_set_border_width (GTK_CONTAINER (video_memory_frame), 2);

  scrolledwindow19 = gtk_scrolled_window_new (NULL, NULL);
  gtk_widget_show (scrolledwindow19);
  gtk_container_add (GTK_CONTAINER (video_memory_frame), scrolledwindow19);
  GTK_WIDGET_UNSET_FLAGS (scrolledwindow19, GTK_CAN_FOCUS);
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (scrolledwindow19), GTK_POLICY_NEVER, GTK_POLICY_NEVER);

  viewport3 = gtk_viewport_new (NULL, NULL);
  gtk_widget_show (viewport3);
  gtk_container_add (GTK_CONTAINER (scrolledwindow19), viewport3);

  text20 = gtk_text_view_new ();
  gtk_widget_show (text20);
  gtk_container_add (GTK_CONTAINER (viewport3), text20);
  gtk_text_view_set_editable (GTK_TEXT_VIEW (text20), FALSE);
  gtk_text_view_set_wrap_mode (GTK_TEXT_VIEW (text20), GTK_WRAP_WORD);

  label34 = gtk_label_new (_("VDP Memory"));
  gtk_widget_show (label34);
  gtk_frame_set_label_widget (GTK_FRAME (video_memory_frame), label34);

  vpaned4 = gtk_vpaned_new ();
  gtk_widget_show (vpaned4);
  gtk_paned_pack2 (GTK_PANED (vpaned3), vpaned4, TRUE, TRUE);
  GTK_WIDGET_UNSET_FLAGS (vpaned4, GTK_CAN_FOCUS);
  gtk_paned_set_position (GTK_PANED (vpaned4), 0);

  graphics_memory_frame = gtk_frame_new (NULL);
  gtk_widget_show (graphics_memory_frame);
  gtk_paned_pack1 (GTK_PANED (vpaned4), graphics_memory_frame, TRUE, FALSE);
  gtk_container_set_border_width (GTK_CONTAINER (graphics_memory_frame), 2);

  scrolledwindow21 = gtk_scrolled_window_new (NULL, NULL);
  gtk_widget_show (scrolledwindow21);
  gtk_container_add (GTK_CONTAINER (graphics_memory_frame), scrolledwindow21);
  GTK_WIDGET_UNSET_FLAGS (scrolledwindow21, GTK_CAN_FOCUS);
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (scrolledwindow21), GTK_POLICY_NEVER, GTK_POLICY_NEVER);

  viewport4 = gtk_viewport_new (NULL, NULL);
  gtk_widget_show (viewport4);
  gtk_container_add (GTK_CONTAINER (scrolledwindow21), viewport4);

  text21 = gtk_text_view_new ();
  gtk_widget_show (text21);
  gtk_container_add (GTK_CONTAINER (viewport4), text21);
  gtk_text_view_set_editable (GTK_TEXT_VIEW (text21), FALSE);
  gtk_text_view_set_wrap_mode (GTK_TEXT_VIEW (text21), GTK_WRAP_WORD);

  label35 = gtk_label_new (_("GROM/GRAM Memory"));
  gtk_widget_show (label35);
  gtk_frame_set_label_widget (GTK_FRAME (graphics_memory_frame), label35);

  speech_memory_frame = gtk_frame_new (NULL);
  gtk_widget_show (speech_memory_frame);
  gtk_paned_pack2 (GTK_PANED (vpaned4), speech_memory_frame, TRUE, FALSE);
  gtk_container_set_border_width (GTK_CONTAINER (speech_memory_frame), 2);

  scrolledwindow23 = gtk_scrolled_window_new (NULL, NULL);
  gtk_widget_show (scrolledwindow23);
  gtk_container_add (GTK_CONTAINER (speech_memory_frame), scrolledwindow23);
  GTK_WIDGET_UNSET_FLAGS (scrolledwindow23, GTK_CAN_FOCUS);
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (scrolledwindow23), GTK_POLICY_NEVER, GTK_POLICY_NEVER);

  viewport5 = gtk_viewport_new (NULL, NULL);
  gtk_widget_show (viewport5);
  gtk_container_add (GTK_CONTAINER (scrolledwindow23), viewport5);

  text22 = gtk_text_view_new ();
  gtk_widget_show (text22);
  gtk_container_add (GTK_CONTAINER (viewport5), text22);
  gtk_text_view_set_editable (GTK_TEXT_VIEW (text22), FALSE);
  gtk_text_view_set_wrap_mode (GTK_TEXT_VIEW (text22), GTK_WRAP_WORD);

  label36 = gtk_label_new (_("Speech Memory"));
  gtk_widget_show (label36);
  gtk_frame_set_label_widget (GTK_FRAME (speech_memory_frame), label36);

  hbox15 = gtk_hbox_new (FALSE, 0);
  gtk_widget_show (hbox15);
  gtk_box_pack_start (GTK_BOX (vbox7), hbox15, FALSE, FALSE, 0);

  vbox15 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox15);
  gtk_box_pack_start (GTK_BOX (hbox15), vbox15, TRUE, TRUE, 0);

  hbox10 = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (hbox10);
  gtk_box_pack_start (GTK_BOX (vbox15), hbox10, TRUE, TRUE, 0);

  button44 = gtk_button_new_with_mnemonic (_("Close"));
  gtk_widget_show (button44);
  gtk_box_pack_start (GTK_BOX (hbox10), button44, FALSE, TRUE, 0);
  GTK_WIDGET_SET_FLAGS (button44, GTK_CAN_DEFAULT);
  gtk_widget_add_accelerator (button44, "clicked", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);

  button41 = gtk_button_new_with_mnemonic (_("_Run"));
  gtk_widget_show (button41);
  gtk_box_pack_start (GTK_BOX (hbox10), button41, FALSE, TRUE, 0);
  GTK_WIDGET_SET_FLAGS (button41, GTK_CAN_DEFAULT);
  gtk_tooltips_set_tip (tooltips, button41, _("Execute emulator with minimal debugger refresh"), NULL);
  gtk_widget_add_accelerator (button41, "clicked", accel_group,
                              GDK_r, 0,
                              GTK_ACCEL_VISIBLE);
  gtk_widget_add_accelerator (button41, "clicked", accel_group,
                              GDK_r, GDK_MOD1_MASK,
                              GTK_ACCEL_VISIBLE);

  button96 = gtk_button_new_with_mnemonic (_("_Step"));
  gtk_widget_show (button96);
  gtk_box_pack_start (GTK_BOX (hbox10), button96, FALSE, TRUE, 0);
  GTK_WIDGET_SET_FLAGS (button96, GTK_CAN_DEFAULT);
  gtk_tooltips_set_tip (tooltips, button96, _("Step one instruction (even into calls) and break"), NULL);
  gtk_widget_add_accelerator (button96, "clicked", accel_group,
                              GDK_s, GDK_MOD1_MASK,
                              GTK_ACCEL_VISIBLE);
  gtk_widget_add_accelerator (button96, "clicked", accel_group,
                              GDK_s, 0,
                              GTK_ACCEL_VISIBLE);

  button97 = gtk_button_new_with_mnemonic (_("_After"));
  gtk_widget_show (button97);
  gtk_box_pack_start (GTK_BOX (hbox10), button97, FALSE, TRUE, 0);
  GTK_WIDGET_SET_FLAGS (button97, GTK_CAN_DEFAULT);
  gtk_tooltips_set_tip (tooltips, button97, _("Execute until the PC reaches the next instruction (ignore conditional jumps)"), NULL);
  gtk_widget_add_accelerator (button97, "clicked", accel_group,
                              GDK_a, 0,
                              GTK_ACCEL_VISIBLE);
  gtk_widget_add_accelerator (button97, "clicked", accel_group,
                              GDK_a, GDK_MOD1_MASK,
                              GTK_ACCEL_VISIBLE);

  hbox14 = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (hbox14);
  gtk_box_pack_start (GTK_BOX (vbox15), hbox14, TRUE, TRUE, 0);

  button89 = gtk_button_new_with_mnemonic (_("_Break"));
  gtk_widget_show (button89);
  gtk_box_pack_start (GTK_BOX (hbox14), button89, FALSE, TRUE, 0);
  GTK_WIDGET_SET_FLAGS (button89, GTK_CAN_DEFAULT);
  gtk_tooltips_set_tip (tooltips, button89, _("Break execution"), NULL);
  gtk_widget_add_accelerator (button89, "clicked", accel_group,
                              GDK_b, GDK_MOD1_MASK,
                              GTK_ACCEL_VISIBLE);
  gtk_widget_add_accelerator (button89, "clicked", accel_group,
                              GDK_b, 0,
                              GTK_ACCEL_VISIBLE);

  button88 = gtk_button_new_with_mnemonic (_("_Walk"));
  gtk_widget_show (button88);
  gtk_box_pack_start (GTK_BOX (hbox14), button88, FALSE, TRUE, 0);
  GTK_WIDGET_SET_FLAGS (button88, GTK_CAN_DEFAULT);
  gtk_tooltips_set_tip (tooltips, button88, _("Execute emulator with maximum debugger refresh"), NULL);
  gtk_widget_add_accelerator (button88, "clicked", accel_group,
                              GDK_w, GDK_MOD1_MASK,
                              GTK_ACCEL_VISIBLE);
  gtk_widget_add_accelerator (button88, "clicked", accel_group,
                              GDK_w, 0,
                              GTK_ACCEL_VISIBLE);

  button40 = gtk_button_new_with_mnemonic (_("_Next"));
  gtk_widget_show (button40);
  gtk_box_pack_start (GTK_BOX (hbox14), button40, FALSE, TRUE, 0);
  GTK_WIDGET_SET_FLAGS (button40, GTK_CAN_DEFAULT);
  gtk_tooltips_set_tip (tooltips, button40, _("Execute until the PC reaches the next instruction (skip over calls)"), NULL);
  gtk_widget_add_accelerator (button40, "clicked", accel_group,
                              GDK_n, 0,
                              GTK_ACCEL_VISIBLE);
  gtk_widget_add_accelerator (button40, "clicked", accel_group,
                              GDK_n, GDK_MOD1_MASK,
                              GTK_ACCEL_VISIBLE);

  label48 = gtk_label_new ("");
  gtk_widget_show (label48);
  gtk_box_pack_start (GTK_BOX (hbox14), label48, FALSE, TRUE, 0);
  gtk_label_set_justify (GTK_LABEL (label48), GTK_JUSTIFY_CENTER);

  table5 = gtk_table_new (3, 2, FALSE);
  gtk_widget_show (table5);
  gtk_box_pack_start (GTK_BOX (hbox15), table5, TRUE, TRUE, 0);

  label43 = gtk_label_new (_("Updates per second"));
  gtk_widget_show (label43);
  gtk_table_attach (GTK_TABLE (table5), label43, 0, 1, 2, 3,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 4, 0);
  gtk_label_set_justify (GTK_LABEL (label43), GTK_JUSTIFY_CENTER);

  spinbutton1_adj = gtk_adjustment_new (1, 0, 100, 1, 10, 10);
  spinbutton1 = gtk_spin_button_new (GTK_ADJUSTMENT (spinbutton1_adj), 1, 0);
  gtk_widget_show (spinbutton1);
  gtk_table_attach (GTK_TABLE (table5), spinbutton1, 1, 2, 2, 3,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 4, 0);
  gtk_tooltips_set_tip (tooltips, spinbutton1, _("Number of times debugger refreshes in \"Run\" mode"), NULL);

  debugger_status_bar = gtk_statusbar_new ();
  gtk_widget_show (debugger_status_bar);
  gtk_box_pack_start (GTK_BOX (vbox7), debugger_status_bar, FALSE, FALSE, 0);

  g_signal_connect ((gpointer) debugger_wp_entry, "activate",
                    G_CALLBACK (on_debugger_editable_activate),
                    "WorkspacePointer");
  g_signal_connect ((gpointer) debugger_pc_entry, "activate",
                    G_CALLBACK (on_debugger_editable_activate),
                    "ProgramCounter");
  g_signal_connect ((gpointer) debugger_st_entry, "activate",
                    G_CALLBACK (on_debugger_editable_activate),
                    "StatusRegister");
  g_signal_connect ((gpointer) button44, "clicked",
                    G_CALLBACK (on_debugger_close_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button41, "clicked",
                    G_CALLBACK (on_debugger_run_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button96, "clicked",
                    G_CALLBACK (on_debugger_step_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button97, "clicked",
                    G_CALLBACK (on_debugger_after_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button89, "clicked",
                    G_CALLBACK (on_debugger_break_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button88, "clicked",
                    G_CALLBACK (on_debugger_walk_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button40, "clicked",
                    G_CALLBACK (on_debugger_next_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) spinbutton1, "realize",
                    G_CALLBACK (on_v9t9_spin_button_realize_value),
                    "GTKDebuggerUpdateRate");
  g_signal_connect ((gpointer) spinbutton1, "changed",
                    G_CALLBACK (on_v9t9_spin_button_changed_value),
                    "GTKDebuggerUpdateRate");

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (debugger_window, debugger_window, "debugger_window");
  GLADE_HOOKUP_OBJECT (debugger_window, vbox7, "vbox7");
  GLADE_HOOKUP_OBJECT (debugger_window, hbox4, "hbox4");
  GLADE_HOOKUP_OBJECT (debugger_window, registers_frame, "registers_frame");
  GLADE_HOOKUP_OBJECT (debugger_window, vbox8, "vbox8");
  GLADE_HOOKUP_OBJECT (debugger_window, debugger_registers_table, "debugger_registers_table");
  GLADE_HOOKUP_OBJECT (debugger_window, wp_edit_hbox, "wp_edit_hbox");
  GLADE_HOOKUP_OBJECT (debugger_window, label6, "label6");
  GLADE_HOOKUP_OBJECT (debugger_window, debugger_wp_entry, "debugger_wp_entry");
  GLADE_HOOKUP_OBJECT (debugger_window, pc_edit_hbox, "pc_edit_hbox");
  GLADE_HOOKUP_OBJECT (debugger_window, label7, "label7");
  GLADE_HOOKUP_OBJECT (debugger_window, debugger_pc_entry, "debugger_pc_entry");
  GLADE_HOOKUP_OBJECT (debugger_window, st_edit_hbox, "st_edit_hbox");
  GLADE_HOOKUP_OBJECT (debugger_window, label8, "label8");
  GLADE_HOOKUP_OBJECT (debugger_window, debugger_st_entry, "debugger_st_entry");
  GLADE_HOOKUP_OBJECT (debugger_window, label30, "label30");
  GLADE_HOOKUP_OBJECT (debugger_window, hpaned1, "hpaned1");
  GLADE_HOOKUP_OBJECT (debugger_window, instructions_frame, "instructions_frame");
  GLADE_HOOKUP_OBJECT (debugger_window, instructions_scrolledwindow, "instructions_scrolledwindow");
  GLADE_HOOKUP_OBJECT (debugger_window, debugger_instruction_box, "debugger_instruction_box");
  GLADE_HOOKUP_OBJECT (debugger_window, label31, "label31");
  GLADE_HOOKUP_OBJECT (debugger_window, vpaned1, "vpaned1");
  GLADE_HOOKUP_OBJECT (debugger_window, cpu_1_memory_frame, "cpu_1_memory_frame");
  GLADE_HOOKUP_OBJECT (debugger_window, scrolledwindow15, "scrolledwindow15");
  GLADE_HOOKUP_OBJECT (debugger_window, viewport1, "viewport1");
  GLADE_HOOKUP_OBJECT (debugger_window, text18, "text18");
  GLADE_HOOKUP_OBJECT (debugger_window, label32, "label32");
  GLADE_HOOKUP_OBJECT (debugger_window, vpaned2, "vpaned2");
  GLADE_HOOKUP_OBJECT (debugger_window, cpu_2_memory_frame, "cpu_2_memory_frame");
  GLADE_HOOKUP_OBJECT (debugger_window, scrolledwindow17, "scrolledwindow17");
  GLADE_HOOKUP_OBJECT (debugger_window, viewport2, "viewport2");
  GLADE_HOOKUP_OBJECT (debugger_window, text19, "text19");
  GLADE_HOOKUP_OBJECT (debugger_window, label33, "label33");
  GLADE_HOOKUP_OBJECT (debugger_window, vpaned3, "vpaned3");
  GLADE_HOOKUP_OBJECT (debugger_window, video_memory_frame, "video_memory_frame");
  GLADE_HOOKUP_OBJECT (debugger_window, scrolledwindow19, "scrolledwindow19");
  GLADE_HOOKUP_OBJECT (debugger_window, viewport3, "viewport3");
  GLADE_HOOKUP_OBJECT (debugger_window, text20, "text20");
  GLADE_HOOKUP_OBJECT (debugger_window, label34, "label34");
  GLADE_HOOKUP_OBJECT (debugger_window, vpaned4, "vpaned4");
  GLADE_HOOKUP_OBJECT (debugger_window, graphics_memory_frame, "graphics_memory_frame");
  GLADE_HOOKUP_OBJECT (debugger_window, scrolledwindow21, "scrolledwindow21");
  GLADE_HOOKUP_OBJECT (debugger_window, viewport4, "viewport4");
  GLADE_HOOKUP_OBJECT (debugger_window, text21, "text21");
  GLADE_HOOKUP_OBJECT (debugger_window, label35, "label35");
  GLADE_HOOKUP_OBJECT (debugger_window, speech_memory_frame, "speech_memory_frame");
  GLADE_HOOKUP_OBJECT (debugger_window, scrolledwindow23, "scrolledwindow23");
  GLADE_HOOKUP_OBJECT (debugger_window, viewport5, "viewport5");
  GLADE_HOOKUP_OBJECT (debugger_window, text22, "text22");
  GLADE_HOOKUP_OBJECT (debugger_window, label36, "label36");
  GLADE_HOOKUP_OBJECT (debugger_window, hbox15, "hbox15");
  GLADE_HOOKUP_OBJECT (debugger_window, vbox15, "vbox15");
  GLADE_HOOKUP_OBJECT (debugger_window, hbox10, "hbox10");
  GLADE_HOOKUP_OBJECT (debugger_window, button44, "button44");
  GLADE_HOOKUP_OBJECT (debugger_window, button41, "button41");
  GLADE_HOOKUP_OBJECT (debugger_window, button96, "button96");
  GLADE_HOOKUP_OBJECT (debugger_window, button97, "button97");
  GLADE_HOOKUP_OBJECT (debugger_window, hbox14, "hbox14");
  GLADE_HOOKUP_OBJECT (debugger_window, button89, "button89");
  GLADE_HOOKUP_OBJECT (debugger_window, button88, "button88");
  GLADE_HOOKUP_OBJECT (debugger_window, button40, "button40");
  GLADE_HOOKUP_OBJECT (debugger_window, label48, "label48");
  GLADE_HOOKUP_OBJECT (debugger_window, table5, "table5");
  GLADE_HOOKUP_OBJECT (debugger_window, label43, "label43");
  GLADE_HOOKUP_OBJECT (debugger_window, spinbutton1, "spinbutton1");
  GLADE_HOOKUP_OBJECT (debugger_window, debugger_status_bar, "debugger_status_bar");
  GLADE_HOOKUP_OBJECT_NO_REF (debugger_window, tooltips, "tooltips");

  gtk_window_add_accel_group (GTK_WINDOW (debugger_window), accel_group);

  return debugger_window;
}

GtkWidget*
create_memory_dialog (void)
{
  GtkWidget *memory_dialog;
  GtkWidget *vbox9;
  GtkWidget *rom_entries_table;
  GtkWidget *console_rom_entry;
  GtkWidget *label30;
  GtkWidget *button35;
  GtkWidget *label31;
  GtkWidget *console_grom_entry;
  GtkWidget *button36;
  GtkWidget *vbox10;
  GtkWidget *checkbutton11;
  GtkWidget *checkbutton10;
  GtkWidget *vbox11;
  GtkWidget *checkbutton13;
  GtkWidget *frame11;
  GtkWidget *module_roms_table;
  GtkWidget *module_rom_entry;
  GtkWidget *module_grom_entry;
  GtkWidget *button53;
  GtkWidget *button54;
  GtkWidget *label33;
  GtkWidget *label34;
  GtkWidget *label35;
  GtkWidget *module_rom1_entry;
  GtkWidget *button55;
  GtkWidget *label36;
  GtkWidget *module_rom2_entry;
  GtkWidget *button56;
  GtkWidget *hbox9;
  GtkWidget *button58;
  GtkWidget *button59;
  GtkWidget *button38;
  GtkWidget *frame10;
  GtkWidget *table4;
  GtkWidget *button48;
  GtkWidget *button57;
  GtkWidget *button47;
  GtkWidget *button95;
  GtkWidget *button45;
  GtkWidget *label49;
  GtkAccelGroup *accel_group;
  GtkTooltips *tooltips;

  tooltips = gtk_tooltips_new ();

  accel_group = gtk_accel_group_new ();

  memory_dialog = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  gtk_window_set_title (GTK_WINDOW (memory_dialog), _("Memory Configuration"));
  gtk_window_set_default_size (GTK_WINDOW (memory_dialog), 400, 400);

  vbox9 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox9);
  gtk_container_add (GTK_CONTAINER (memory_dialog), vbox9);

  rom_entries_table = gtk_table_new (2, 3, FALSE);
  gtk_widget_show (rom_entries_table);
  gtk_box_pack_start (GTK_BOX (vbox9), rom_entries_table, TRUE, TRUE, 0);

  console_rom_entry = gtk_entry_new ();
  gtk_widget_show (console_rom_entry);
  gtk_table_attach (GTK_TABLE (rom_entries_table), console_rom_entry, 1, 2, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  label30 = gtk_label_new_with_mnemonic (_("Console _ROM"));
  gtk_widget_show (label30);
  gtk_table_attach (GTK_TABLE (rom_entries_table), label30, 0, 1, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_label_set_justify (GTK_LABEL (label30), GTK_JUSTIFY_CENTER);

  button35 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button35);
  gtk_table_attach (GTK_TABLE (rom_entries_table), button35, 2, 3, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button35), 4);

  label31 = gtk_label_new_with_mnemonic (_("Console _GROM"));
  gtk_widget_show (label31);
  gtk_table_attach (GTK_TABLE (rom_entries_table), label31, 0, 1, 1, 2,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_label_set_justify (GTK_LABEL (label31), GTK_JUSTIFY_CENTER);

  console_grom_entry = gtk_entry_new ();
  gtk_widget_show (console_grom_entry);
  gtk_table_attach (GTK_TABLE (rom_entries_table), console_grom_entry, 1, 2, 1, 2,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  button36 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button36);
  gtk_table_attach (GTK_TABLE (rom_entries_table), button36, 2, 3, 1, 2,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button36), 4);

  vbox10 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox10);
  gtk_box_pack_start (GTK_BOX (vbox9), vbox10, TRUE, TRUE, 0);

  checkbutton11 = gtk_check_button_new_with_mnemonic (_("Use 32K _extended memory"));
  gtk_widget_show (checkbutton11);
  gtk_box_pack_start (GTK_BOX (vbox10), checkbutton11, FALSE, FALSE, 0);

  checkbutton10 = gtk_check_button_new_with_mnemonic (_("Enable Gene_ve-style RAM in >8000->82FF range"));
  gtk_widget_show (checkbutton10);
  gtk_box_pack_start (GTK_BOX (vbox10), checkbutton10, FALSE, FALSE, 0);

  vbox11 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox11);
  gtk_box_pack_start (GTK_BOX (vbox10), vbox11, TRUE, TRUE, 0);

  checkbutton13 = gtk_check_button_new_with_mnemonic (_("Load custom module ROMs"));
  gtk_widget_show (checkbutton13);
  gtk_box_pack_start (GTK_BOX (vbox11), checkbutton13, FALSE, FALSE, 0);

  frame11 = gtk_frame_new (NULL);
  gtk_widget_show (frame11);
  gtk_box_pack_start (GTK_BOX (vbox11), frame11, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (frame11), 2);

  module_roms_table = gtk_table_new (4, 3, FALSE);
  gtk_widget_show (module_roms_table);
  gtk_container_add (GTK_CONTAINER (frame11), module_roms_table);

  module_rom_entry = gtk_entry_new ();
  gtk_widget_show (module_rom_entry);
  gtk_table_attach (GTK_TABLE (module_roms_table), module_rom_entry, 1, 2, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  module_grom_entry = gtk_entry_new ();
  gtk_widget_show (module_grom_entry);
  gtk_table_attach (GTK_TABLE (module_roms_table), module_grom_entry, 1, 2, 1, 2,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  button53 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button53);
  gtk_table_attach (GTK_TABLE (module_roms_table), button53, 2, 3, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button53), 4);

  button54 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button54);
  gtk_table_attach (GTK_TABLE (module_roms_table), button54, 2, 3, 1, 2,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button54), 4);

  label33 = gtk_label_new_with_mnemonic (_("_Module ROM"));
  gtk_widget_show (label33);
  gtk_table_attach (GTK_TABLE (module_roms_table), label33, 0, 1, 0, 1,
                    (GtkAttachOptions) (GTK_EXPAND),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_label_set_justify (GTK_LABEL (label33), GTK_JUSTIFY_CENTER);

  label34 = gtk_label_new_with_mnemonic (_("M_odule GROM"));
  gtk_widget_show (label34);
  gtk_table_attach (GTK_TABLE (module_roms_table), label34, 0, 1, 1, 2,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_label_set_justify (GTK_LABEL (label34), GTK_JUSTIFY_CENTER);

  label35 = gtk_label_new_with_mnemonic (_("Module ROM (bank _1)"));
  gtk_widget_show (label35);
  gtk_table_attach (GTK_TABLE (module_roms_table), label35, 0, 1, 2, 3,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_label_set_justify (GTK_LABEL (label35), GTK_JUSTIFY_CENTER);

  module_rom1_entry = gtk_entry_new ();
  gtk_widget_show (module_rom1_entry);
  gtk_table_attach (GTK_TABLE (module_roms_table), module_rom1_entry, 1, 2, 2, 3,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  button55 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button55);
  gtk_table_attach (GTK_TABLE (module_roms_table), button55, 2, 3, 2, 3,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button55), 4);

  label36 = gtk_label_new_with_mnemonic (_("Module ROM (bank _2)"));
  gtk_widget_show (label36);
  gtk_table_attach (GTK_TABLE (module_roms_table), label36, 0, 1, 3, 4,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_label_set_justify (GTK_LABEL (label36), GTK_JUSTIFY_CENTER);

  module_rom2_entry = gtk_entry_new ();
  gtk_widget_show (module_rom2_entry);
  gtk_table_attach (GTK_TABLE (module_roms_table), module_rom2_entry, 1, 2, 3, 4,
                    (GtkAttachOptions) (GTK_EXPAND | GTK_FILL),
                    (GtkAttachOptions) (0), 0, 0);

  button56 = gtk_button_new_with_mnemonic (_("Choose..."));
  gtk_widget_show (button56);
  gtk_table_attach (GTK_TABLE (module_roms_table), button56, 2, 3, 3, 4,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button56), 4);

  hbox9 = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (hbox9);
  gtk_box_pack_end (GTK_BOX (vbox9), hbox9, FALSE, FALSE, 4);

  button58 = gtk_button_new_with_mnemonic (_("Resume"));
  gtk_widget_show (button58);
  gtk_box_pack_start (GTK_BOX (hbox9), button58, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button58), 4);
  gtk_tooltips_set_tip (tooltips, button58, _("Continue emulation"), NULL);

  button59 = gtk_button_new_with_mnemonic (_("Boot and Run"));
  gtk_widget_show (button59);
  gtk_box_pack_start (GTK_BOX (hbox9), button59, TRUE, TRUE, 4);
  gtk_container_set_border_width (GTK_CONTAINER (button59), 4);
  gtk_tooltips_set_tip (tooltips, button59, _("Close window and reset computer"), NULL);
  gtk_widget_add_accelerator (button59, "clicked", accel_group,
                              GDK_Return, 0,
                              GTK_ACCEL_VISIBLE);

  button38 = gtk_button_new_with_mnemonic (_("Close"));
  gtk_widget_show (button38);
  gtk_box_pack_start (GTK_BOX (hbox9), button38, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button38), 4);
  gtk_tooltips_set_tip (tooltips, button38, _("Close window and continue emulation"), NULL);
  gtk_widget_add_accelerator (button38, "clicked", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);

  frame10 = gtk_frame_new (NULL);
  gtk_widget_show (frame10);
  gtk_box_pack_start (GTK_BOX (vbox9), frame10, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (frame10), 4);

  table4 = gtk_table_new (3, 3, TRUE);
  gtk_widget_show (table4);
  gtk_container_add (GTK_CONTAINER (frame10), table4);

  button48 = gtk_button_new_with_mnemonic (_("Explore memory map"));
  gtk_widget_show (button48);
  gtk_table_attach (GTK_TABLE (table4), button48, 0, 1, 0, 1,
                    (GtkAttachOptions) (GTK_FILL),
                    (GtkAttachOptions) (0), 8, 0);

  button57 = gtk_button_new_with_mnemonic (_("Unload module"));
  gtk_widget_show (button57);
  gtk_table_attach (GTK_TABLE (table4), button57, 1, 2, 2, 3,
                    (GtkAttachOptions) (GTK_FILL),
                    (GtkAttachOptions) (0), 8, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button57), 4);
  gtk_tooltips_set_tip (tooltips, button57, _("Unload all ROMs and RAMs associated with a module"), NULL);

  button47 = gtk_button_new_with_mnemonic (_("Save volatile RAM"));
  gtk_widget_show (button47);
  gtk_table_attach (GTK_TABLE (table4), button47, 2, 3, 0, 1,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button47), 4);
  gtk_tooltips_set_tip (tooltips, button47, _("These RAMs are saved automatically at certain points"), NULL);

  button95 = gtk_button_new_with_mnemonic (_("Load volatile RAM"));
  gtk_widget_show (button95);
  gtk_table_attach (GTK_TABLE (table4), button95, 2, 3, 1, 2,
                    (GtkAttachOptions) (0),
                    (GtkAttachOptions) (0), 0, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button95), 4);
  gtk_tooltips_set_tip (tooltips, button95, _("These RAMs are loaded automatically at certain points"), NULL);

  button45 = gtk_button_new_with_mnemonic (_("Default memory map"));
  gtk_widget_show (button45);
  gtk_table_attach (GTK_TABLE (table4), button45, 0, 1, 1, 2,
                    (GtkAttachOptions) (GTK_FILL),
                    (GtkAttachOptions) (0), 8, 0);
  gtk_tooltips_set_tip (tooltips, button45, _("Sets up RAM and memory-mapped areas to standard 99/4A settings in case there was a mishap with DefineMemory"), NULL);

  label49 = gtk_label_new (_("Advanced options"));
  gtk_widget_show (label49);
  gtk_frame_set_label_widget (GTK_FRAME (frame10), label49);

  g_signal_connect ((gpointer) console_rom_entry, "activate",
                    G_CALLBACK (on_v9t9_rom_entry_activate),
                    "ConsoleROMFilename");
  g_signal_connect ((gpointer) console_rom_entry, "realize",
                    G_CALLBACK (on_v9t9_rom_entry_realize),
                    "ConsoleROMFilename");
  g_signal_connect ((gpointer) button35, "clicked",
                    G_CALLBACK (on_v9t9_rom_button_clicked),
                    console_rom_entry);
  g_signal_connect ((gpointer) console_grom_entry, "activate",
                    G_CALLBACK (on_v9t9_rom_entry_activate),
                    "ConsoleGROMFilename");
  g_signal_connect ((gpointer) console_grom_entry, "realize",
                    G_CALLBACK (on_v9t9_rom_entry_realize),
                    "ConsoleGROMFilename");
  g_signal_connect ((gpointer) button36, "clicked",
                    G_CALLBACK (on_v9t9_rom_button_clicked),
                    on_v9t9_rom_button_clicked);
  g_signal_connect ((gpointer) checkbutton11, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                    "MemoryExpansion32K");
  g_signal_connect ((gpointer) checkbutton11, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_active),
                    "MemoryExpansion32K");
  g_signal_connect ((gpointer) checkbutton10, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                    "ExtraConsoleRAM");
  g_signal_connect ((gpointer) checkbutton10, "realize",
                    G_CALLBACK (on_v9t9_togglebutton_realize_active),
                    "ExtraConsoleRAM");
  g_signal_connect ((gpointer) checkbutton13, "toggled",
                    G_CALLBACK (on_v9t9_togglebutton_toggled_widget_enable),
                    module_roms_table);
  g_signal_connect ((gpointer) checkbutton13, "realize",
                    G_CALLBACK (on_memory_config_module_rom_button_realize),
                    module_roms_table);
  g_signal_connect ((gpointer) checkbutton13, "toggled",
                    G_CALLBACK (on_memory_config_module_rom_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) module_rom_entry, "activate",
                    G_CALLBACK (on_memory_config_banked_module_deactivate),
                    module_rom1_entry);
  g_signal_connect ((gpointer) module_rom_entry, "realize",
                    G_CALLBACK (on_v9t9_rom_entry_realize),
                    "ModuleROMFilename");
  g_signal_connect ((gpointer) module_rom_entry, "activate",
                    G_CALLBACK (on_memory_config_banked_module_deactivate),
                    module_rom2_entry);
  g_signal_connect ((gpointer) module_rom_entry, "activate",
                    G_CALLBACK (on_v9t9_rom_entry_activate),
                    "ModuleROMFilename");
  g_signal_connect ((gpointer) module_grom_entry, "activate",
                    G_CALLBACK (on_v9t9_rom_entry_activate),
                    "ModuleGROMFilename");
  g_signal_connect ((gpointer) module_grom_entry, "realize",
                    G_CALLBACK (on_v9t9_rom_entry_realize),
                    "ModuleGROMFilename");
  g_signal_connect ((gpointer) button53, "clicked",
                    G_CALLBACK (on_v9t9_rom_button_clicked),
                    module_rom_entry);
  g_signal_connect ((gpointer) button54, "clicked",
                    G_CALLBACK (on_v9t9_rom_button_clicked),
                    module_grom_entry);
  g_signal_connect ((gpointer) module_rom1_entry, "activate",
                    G_CALLBACK (on_module_config_banked_module_activate),
                    module_rom_entry);
  g_signal_connect ((gpointer) module_rom1_entry, "realize",
                    G_CALLBACK (on_v9t9_rom_entry_realize),
                    "ModuleROM1Filename");
  g_signal_connect ((gpointer) module_rom1_entry, "activate",
                    G_CALLBACK (on_v9t9_rom_entry_activate),
                    "ModuleROM1Filename");
  g_signal_connect ((gpointer) button55, "clicked",
                    G_CALLBACK (on_v9t9_rom_button_clicked),
                    module_rom1_entry);
  g_signal_connect ((gpointer) module_rom2_entry, "activate",
                    G_CALLBACK (on_module_config_banked_module_activate),
                    module_rom_entry);
  g_signal_connect ((gpointer) module_rom2_entry, "realize",
                    G_CALLBACK (on_v9t9_rom_entry_realize),
                    "ModuleROM2Filename");
  g_signal_connect ((gpointer) module_rom2_entry, "activate",
                    G_CALLBACK (on_v9t9_rom_entry_activate),
                    "ModuleROM2Filename");
  g_signal_connect ((gpointer) button56, "clicked",
                    G_CALLBACK (on_v9t9_rom_button_clicked),
                    module_rom2_entry);
  g_signal_connect ((gpointer) button58, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "PauseComputer off");
  g_signal_connect ((gpointer) button59, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "ResetComputer");
  g_signal_connect ((gpointer) button59, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "ResetComputer");
  g_signal_connect ((gpointer) button59, "clicked",
                    G_CALLBACK (on_memory_dialog_close_button_clicked),
                    "ResetComputer");
  g_signal_connect ((gpointer) button38, "clicked",
                    G_CALLBACK (on_memory_dialog_close_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button48, "clicked",
                    G_CALLBACK (on_change_memory_map_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button57, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "UnloadModule");
  g_signal_connect ((gpointer) button47, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "SaveMemory");
  g_signal_connect ((gpointer) button95, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "LoadMemory");
  g_signal_connect ((gpointer) button45, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "DefaultMemoryMap");

  gtk_label_set_mnemonic_widget (GTK_LABEL (label30), console_rom_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label31), console_grom_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label33), module_rom_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label34), module_grom_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label35), module_rom1_entry);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label36), module_rom2_entry);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (memory_dialog, memory_dialog, "memory_dialog");
  GLADE_HOOKUP_OBJECT (memory_dialog, vbox9, "vbox9");
  GLADE_HOOKUP_OBJECT (memory_dialog, rom_entries_table, "rom_entries_table");
  GLADE_HOOKUP_OBJECT (memory_dialog, console_rom_entry, "console_rom_entry");
  GLADE_HOOKUP_OBJECT (memory_dialog, label30, "label30");
  GLADE_HOOKUP_OBJECT (memory_dialog, button35, "button35");
  GLADE_HOOKUP_OBJECT (memory_dialog, label31, "label31");
  GLADE_HOOKUP_OBJECT (memory_dialog, console_grom_entry, "console_grom_entry");
  GLADE_HOOKUP_OBJECT (memory_dialog, button36, "button36");
  GLADE_HOOKUP_OBJECT (memory_dialog, vbox10, "vbox10");
  GLADE_HOOKUP_OBJECT (memory_dialog, checkbutton11, "checkbutton11");
  GLADE_HOOKUP_OBJECT (memory_dialog, checkbutton10, "checkbutton10");
  GLADE_HOOKUP_OBJECT (memory_dialog, vbox11, "vbox11");
  GLADE_HOOKUP_OBJECT (memory_dialog, checkbutton13, "checkbutton13");
  GLADE_HOOKUP_OBJECT (memory_dialog, frame11, "frame11");
  GLADE_HOOKUP_OBJECT (memory_dialog, module_roms_table, "module_roms_table");
  GLADE_HOOKUP_OBJECT (memory_dialog, module_rom_entry, "module_rom_entry");
  GLADE_HOOKUP_OBJECT (memory_dialog, module_grom_entry, "module_grom_entry");
  GLADE_HOOKUP_OBJECT (memory_dialog, button53, "button53");
  GLADE_HOOKUP_OBJECT (memory_dialog, button54, "button54");
  GLADE_HOOKUP_OBJECT (memory_dialog, label33, "label33");
  GLADE_HOOKUP_OBJECT (memory_dialog, label34, "label34");
  GLADE_HOOKUP_OBJECT (memory_dialog, label35, "label35");
  GLADE_HOOKUP_OBJECT (memory_dialog, module_rom1_entry, "module_rom1_entry");
  GLADE_HOOKUP_OBJECT (memory_dialog, button55, "button55");
  GLADE_HOOKUP_OBJECT (memory_dialog, label36, "label36");
  GLADE_HOOKUP_OBJECT (memory_dialog, module_rom2_entry, "module_rom2_entry");
  GLADE_HOOKUP_OBJECT (memory_dialog, button56, "button56");
  GLADE_HOOKUP_OBJECT (memory_dialog, hbox9, "hbox9");
  GLADE_HOOKUP_OBJECT (memory_dialog, button58, "button58");
  GLADE_HOOKUP_OBJECT (memory_dialog, button59, "button59");
  GLADE_HOOKUP_OBJECT (memory_dialog, button38, "button38");
  GLADE_HOOKUP_OBJECT (memory_dialog, frame10, "frame10");
  GLADE_HOOKUP_OBJECT (memory_dialog, table4, "table4");
  GLADE_HOOKUP_OBJECT (memory_dialog, button48, "button48");
  GLADE_HOOKUP_OBJECT (memory_dialog, button57, "button57");
  GLADE_HOOKUP_OBJECT (memory_dialog, button47, "button47");
  GLADE_HOOKUP_OBJECT (memory_dialog, button95, "button95");
  GLADE_HOOKUP_OBJECT (memory_dialog, button45, "button45");
  GLADE_HOOKUP_OBJECT (memory_dialog, label49, "label49");
  GLADE_HOOKUP_OBJECT_NO_REF (memory_dialog, tooltips, "tooltips");

  gtk_window_add_accel_group (GTK_WINDOW (memory_dialog), accel_group);

  return memory_dialog;
}

GtkWidget*
create_options_dialog (void)
{
  GtkWidget *options_dialog;
  GtkWidget *vbox12;
  GtkWidget *vbox13;
  GtkWidget *checkbutton15;
  GtkWidget *checkbutton17;
  GtkWidget *hseparator4;
  GtkWidget *checkbutton14;
  GtkWidget *clock_speed_hbox;
  GtkWidget *label42;
  GtkObject *clock_speed_spin_button_adj;
  GtkWidget *clock_speed_spin_button;
  GtkWidget *button70;
  GtkWidget *delay_hbox;
  GtkWidget *label39;
  GtkObject *delay_spin_button_adj;
  GtkWidget *delay_spin_button;
  GtkWidget *button71;
  GtkWidget *video_refresh_hbox;
  GtkWidget *label40;
  GtkObject *video_refresh_spin_button_adj;
  GtkWidget *video_refresh_spin_button;
  GtkWidget *button72;
  GtkWidget *vdp_interrupt_hbox;
  GtkWidget *label41;
  GtkObject *vdp_interrupt_spin_button_adj;
  GtkWidget *vdp_interrupt_spin_button;
  GtkWidget *button73;
  GtkWidget *hbox12;
  GtkWidget *button69;
  GtkWidget *hseparator5;
  GtkAccelGroup *accel_group;
  GtkTooltips *tooltips;

  tooltips = gtk_tooltips_new ();

  accel_group = gtk_accel_group_new ();

  options_dialog = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  gtk_window_set_title (GTK_WINDOW (options_dialog), _("Basic Options"));

  vbox12 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox12);
  gtk_container_add (GTK_CONTAINER (options_dialog), vbox12);
  gtk_container_set_border_width (GTK_CONTAINER (vbox12), 4);

  vbox13 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox13);
  gtk_box_pack_start (GTK_BOX (vbox12), vbox13, TRUE, TRUE, 0);

  checkbutton15 = gtk_check_button_new_with_mnemonic (_("Enable _sound"));
  gtk_widget_show (checkbutton15);
  gtk_box_pack_start (GTK_BOX (vbox13), checkbutton15, FALSE, FALSE, 0);

  checkbutton17 = gtk_check_button_new_with_mnemonic (_("Enable s_peech"));
  gtk_widget_show (checkbutton17);
  gtk_box_pack_start (GTK_BOX (vbox13), checkbutton17, FALSE, FALSE, 0);

  hseparator4 = gtk_hseparator_new ();
  gtk_widget_show (hseparator4);
  gtk_box_pack_start (GTK_BOX (vbox13), hseparator4, TRUE, TRUE, 0);

  checkbutton14 = gtk_check_button_new_with_mnemonic (_("_Real-time emulation"));
  gtk_widget_show (checkbutton14);
  gtk_box_pack_start (GTK_BOX (vbox13), checkbutton14, FALSE, FALSE, 0);

  clock_speed_hbox = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (clock_speed_hbox);
  gtk_box_pack_start (GTK_BOX (vbox13), clock_speed_hbox, TRUE, TRUE, 0);

  label42 = gtk_label_new_with_mnemonic (_("_9900 Clock Speed"));
  gtk_widget_show (label42);
  gtk_box_pack_start (GTK_BOX (clock_speed_hbox), label42, TRUE, TRUE, 0);
  gtk_label_set_justify (GTK_LABEL (label42), GTK_JUSTIFY_CENTER);
  gtk_misc_set_alignment (GTK_MISC (label42), 1, 0.5);

  clock_speed_spin_button_adj = gtk_adjustment_new (3.3e+06, 1, 1e+09, 10000, 1e+06, 1e+06);
  clock_speed_spin_button = gtk_spin_button_new (GTK_ADJUSTMENT (clock_speed_spin_button_adj), 1, 0);
  gtk_widget_show (clock_speed_spin_button);
  gtk_box_pack_start (GTK_BOX (clock_speed_hbox), clock_speed_spin_button, TRUE, TRUE, 4);
  gtk_tooltips_set_tip (tooltips, clock_speed_spin_button, _("This affects the maximum execution speed when real-time emulation is enavbled"), NULL);

  button70 = gtk_button_new_with_mnemonic (_("Default"));
  gtk_widget_show (button70);
  gtk_box_pack_start (GTK_BOX (clock_speed_hbox), button70, FALSE, FALSE, 0);

  delay_hbox = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (delay_hbox);
  gtk_box_pack_start (GTK_BOX (vbox13), delay_hbox, TRUE, TRUE, 0);

  label39 = gtk_label_new_with_mnemonic (_("Instruction _delay"));
  gtk_widget_show (label39);
  gtk_box_pack_start (GTK_BOX (delay_hbox), label39, TRUE, TRUE, 0);
  gtk_misc_set_alignment (GTK_MISC (label39), 1, 0.5);

  delay_spin_button_adj = gtk_adjustment_new (0, 0, 1e+09, 100, 1000, 1000);
  delay_spin_button = gtk_spin_button_new (GTK_ADJUSTMENT (delay_spin_button_adj), 1, 0);
  gtk_widget_show (delay_spin_button);
  gtk_box_pack_start (GTK_BOX (delay_hbox), delay_spin_button, TRUE, TRUE, 4);
  gtk_tooltips_set_tip (tooltips, delay_spin_button, _("This value is highly CPU-dependent"), NULL);

  button71 = gtk_button_new_with_mnemonic (_("Default"));
  gtk_widget_show (button71);
  gtk_box_pack_start (GTK_BOX (delay_hbox), button71, FALSE, FALSE, 0);

  video_refresh_hbox = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (video_refresh_hbox);
  gtk_box_pack_start (GTK_BOX (vbox13), video_refresh_hbox, FALSE, FALSE, 0);

  label40 = gtk_label_new_with_mnemonic (_("_Video refresh rate"));
  gtk_widget_show (label40);
  gtk_box_pack_start (GTK_BOX (video_refresh_hbox), label40, TRUE, TRUE, 0);
  gtk_label_set_justify (GTK_LABEL (label40), GTK_JUSTIFY_CENTER);
  gtk_misc_set_alignment (GTK_MISC (label40), 0, 0.5);

  video_refresh_spin_button_adj = gtk_adjustment_new (30, 0, 100, 1, 10, 10);
  video_refresh_spin_button = gtk_spin_button_new (GTK_ADJUSTMENT (video_refresh_spin_button_adj), 1, 0);
  gtk_widget_show (video_refresh_spin_button);
  gtk_box_pack_start (GTK_BOX (video_refresh_hbox), video_refresh_spin_button, TRUE, TRUE, 4);
  gtk_tooltips_set_tip (tooltips, video_refresh_spin_button, _("This controls how often the changed parts of the 99/4A screen are redrawn"), NULL);

  button72 = gtk_button_new_with_mnemonic (_("Default"));
  gtk_widget_show (button72);
  gtk_box_pack_start (GTK_BOX (video_refresh_hbox), button72, FALSE, FALSE, 0);

  vdp_interrupt_hbox = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (vdp_interrupt_hbox);
  gtk_box_pack_start (GTK_BOX (vbox13), vdp_interrupt_hbox, TRUE, TRUE, 0);

  label41 = gtk_label_new_with_mnemonic (_("VDP _interrupt rate"));
  gtk_widget_show (label41);
  gtk_box_pack_start (GTK_BOX (vdp_interrupt_hbox), label41, TRUE, TRUE, 0);
  gtk_label_set_justify (GTK_LABEL (label41), GTK_JUSTIFY_CENTER);
  gtk_misc_set_alignment (GTK_MISC (label41), 7.45058e-09, 0.5);

  vdp_interrupt_spin_button_adj = gtk_adjustment_new (60, 0, 100, 1, 10, 10);
  vdp_interrupt_spin_button = gtk_spin_button_new (GTK_ADJUSTMENT (vdp_interrupt_spin_button_adj), 1, 0);
  gtk_widget_show (vdp_interrupt_spin_button);
  gtk_box_pack_start (GTK_BOX (vdp_interrupt_hbox), vdp_interrupt_spin_button, TRUE, TRUE, 4);
  gtk_tooltips_set_tip (tooltips, vdp_interrupt_spin_button, _("This affects sprite motion and timing"), NULL);

  button73 = gtk_button_new_with_mnemonic (_("Default"));
  gtk_widget_show (button73);
  gtk_box_pack_start (GTK_BOX (vdp_interrupt_hbox), button73, FALSE, FALSE, 0);

  hbox12 = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (hbox12);
  gtk_box_pack_end (GTK_BOX (vbox12), hbox12, FALSE, FALSE, 4);

  button69 = gtk_button_new_with_mnemonic (_("Close"));
  gtk_widget_show (button69);
  gtk_box_pack_start (GTK_BOX (hbox12), button69, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button69), 4);
  gtk_tooltips_set_tip (tooltips, button69, _("Close window and continue emulation"), NULL);
  gtk_widget_add_accelerator (button69, "clicked", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);

  hseparator5 = gtk_hseparator_new ();
  gtk_widget_show (hseparator5);
  gtk_box_pack_start (GTK_BOX (vbox12), hseparator5, TRUE, TRUE, 0);

  g_signal_connect ((gpointer) checkbutton15, "toggled",
                            G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                           ("PlaySound"));
  g_signal_connect ((gpointer) checkbutton15, "realize",
                            G_CALLBACK (on_v9t9_togglebutton_realize_active),
                           ("PlaySound"));
  g_signal_connect ((gpointer) checkbutton17, "toggled",
                            G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                           ("PlaySpeech"));
  g_signal_connect ((gpointer) checkbutton17, "realize",
                            G_CALLBACK (on_v9t9_togglebutton_realize_active),
                           ("PlaySpeech"));
  g_signal_connect ((gpointer) checkbutton14, "toggled",
                            G_CALLBACK (on_v9t9_togglebutton_toggled_command_toggle),
                           ("RealTimeEmulation"));
  g_signal_connect ((gpointer) checkbutton14, "realize",
                            G_CALLBACK (on_v9t9_togglebutton_realize_active),
                           ("RealTimeEmulation"));
  g_signal_connect ((gpointer) checkbutton14, "toggled",
                            G_CALLBACK (on_v9t9_togglebutton_toggled_widget_enable_not),
                            GTK_OBJECT ((gpointer)delay_hbox));
  g_signal_connect ((gpointer) checkbutton14, "realize",
                            G_CALLBACK (on_v9t9_togglebutton_realize_widget_enable_not),
                            GTK_OBJECT ((gpointer)delay_hbox));
  g_signal_connect ((gpointer) checkbutton14, "toggled",
                            G_CALLBACK (on_v9t9_togglebutton_toggled_widget_enable),
                            GTK_OBJECT ((gpointer)clock_speed_hbox));
  g_signal_connect ((gpointer) checkbutton14, "realize",
                            G_CALLBACK (on_v9t9_togglebutton_realize_widget_enable),
                            GTK_OBJECT ((gpointer)clock_speed_hbox));
  g_signal_connect_data ((gpointer) clock_speed_spin_button, "activate",
                         G_CALLBACK (on_v9t9_spin_button_changed_value),
                        ("BaseClockHZ"),
                         NULL, G_CONNECT_AFTER);
  g_signal_connect ((gpointer) clock_speed_spin_button, "realize",
                            G_CALLBACK (on_v9t9_spin_button_realize_value),
                           ("BaseClockHZ"));
  g_signal_connect ((gpointer) clock_speed_spin_button, "show",
                    G_CALLBACK (on_v9t9_spin_button_realize_value),
                    "BaseClockHZ");
  g_signal_connect_after ((gpointer) clock_speed_spin_button, "changed",
                          G_CALLBACK (on_v9t9_spin_button_changed_value),
                          "BaseClockHZ");
  g_signal_connect ((gpointer) button70, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "BaseClockHZ 3300000");
  g_signal_connect ((gpointer) button70, "clicked",
                            G_CALLBACK (on_v9t9_button_clicked_realize_widget),
                            GTK_OBJECT ((gpointer)clock_speed_spin_button));
  g_signal_connect_after ((gpointer) delay_spin_button, "activate",
                          G_CALLBACK (on_v9t9_spin_button_changed_value),
                          "DelayBetweenInstructions");
  g_signal_connect ((gpointer) delay_spin_button, "realize",
                    G_CALLBACK (on_v9t9_spin_button_realize_value),
                    "DelayBetweenInstructions");
  g_signal_connect ((gpointer) delay_spin_button, "show",
                    G_CALLBACK (on_v9t9_spin_button_realize_value),
                    "DelayBetweenInstructions");
  g_signal_connect_after ((gpointer) delay_spin_button, "changed",
                          G_CALLBACK (on_v9t9_spin_button_changed_value),
                          "DelayBetweenInstructions");
  g_signal_connect ((gpointer) button71, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "DelayBetweenInstructions 0");
  g_signal_connect ((gpointer) button71, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked_realize_widget),
                    delay_spin_button);
  g_signal_connect_after ((gpointer) video_refresh_spin_button, "activate",
                          G_CALLBACK (on_v9t9_spin_button_changed_value),
                          "VideoUpdateSpeed");
  g_signal_connect ((gpointer) video_refresh_spin_button, "realize",
                    G_CALLBACK (on_v9t9_spin_button_realize_value),
                    "VideoUpdateSpeed");
  g_signal_connect ((gpointer) video_refresh_spin_button, "show",
                    G_CALLBACK (on_v9t9_spin_button_realize_value),
                    "VideoUpdateSpeed");
  g_signal_connect_after ((gpointer) video_refresh_spin_button, "changed",
                          G_CALLBACK (on_v9t9_spin_button_changed_value),
                          "VideoUpdateSpeed");
  g_signal_connect ((gpointer) button72, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "VideoUpdateSpeed 30");
  g_signal_connect ((gpointer) button72, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked_realize_widget),
                    video_refresh_spin_button);
  g_signal_connect_after ((gpointer) vdp_interrupt_spin_button, "activate",
                          G_CALLBACK (on_v9t9_spin_button_changed_value),
                          "VDPInterruptRate");
  g_signal_connect ((gpointer) vdp_interrupt_spin_button, "realize",
                    G_CALLBACK (on_v9t9_spin_button_realize_value),
                    "VDPInterruptRate");
  g_signal_connect ((gpointer) vdp_interrupt_spin_button, "show",
                    G_CALLBACK (on_v9t9_spin_button_realize_value),
                    "VDPInterruptRate");
  g_signal_connect_after ((gpointer) vdp_interrupt_spin_button, "changed",
                          G_CALLBACK (on_v9t9_spin_button_changed_value),
                          "VDPInterruptRate");
  g_signal_connect ((gpointer) button73, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "VDPInterruptRate 60");
  g_signal_connect ((gpointer) button73, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked_realize_widget),
                    vdp_interrupt_spin_button);
  g_signal_connect ((gpointer) button69, "clicked",
                    G_CALLBACK (on_option_dialog_close_button_clicked),
                    NULL);

  gtk_label_set_mnemonic_widget (GTK_LABEL (label42), clock_speed_spin_button);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label39), delay_spin_button);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label40), video_refresh_spin_button);
  gtk_label_set_mnemonic_widget (GTK_LABEL (label41), vdp_interrupt_spin_button);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (options_dialog, options_dialog, "options_dialog");
  GLADE_HOOKUP_OBJECT (options_dialog, vbox12, "vbox12");
  GLADE_HOOKUP_OBJECT (options_dialog, vbox13, "vbox13");
  GLADE_HOOKUP_OBJECT (options_dialog, checkbutton15, "checkbutton15");
  GLADE_HOOKUP_OBJECT (options_dialog, checkbutton17, "checkbutton17");
  GLADE_HOOKUP_OBJECT (options_dialog, hseparator4, "hseparator4");
  GLADE_HOOKUP_OBJECT (options_dialog, checkbutton14, "checkbutton14");
  GLADE_HOOKUP_OBJECT (options_dialog, clock_speed_hbox, "clock_speed_hbox");
  GLADE_HOOKUP_OBJECT (options_dialog, label42, "label42");
  GLADE_HOOKUP_OBJECT (options_dialog, clock_speed_spin_button, "clock_speed_spin_button");
  GLADE_HOOKUP_OBJECT (options_dialog, button70, "button70");
  GLADE_HOOKUP_OBJECT (options_dialog, delay_hbox, "delay_hbox");
  GLADE_HOOKUP_OBJECT (options_dialog, label39, "label39");
  GLADE_HOOKUP_OBJECT (options_dialog, delay_spin_button, "delay_spin_button");
  GLADE_HOOKUP_OBJECT (options_dialog, button71, "button71");
  GLADE_HOOKUP_OBJECT (options_dialog, video_refresh_hbox, "video_refresh_hbox");
  GLADE_HOOKUP_OBJECT (options_dialog, label40, "label40");
  GLADE_HOOKUP_OBJECT (options_dialog, video_refresh_spin_button, "video_refresh_spin_button");
  GLADE_HOOKUP_OBJECT (options_dialog, button72, "button72");
  GLADE_HOOKUP_OBJECT (options_dialog, vdp_interrupt_hbox, "vdp_interrupt_hbox");
  GLADE_HOOKUP_OBJECT (options_dialog, label41, "label41");
  GLADE_HOOKUP_OBJECT (options_dialog, vdp_interrupt_spin_button, "vdp_interrupt_spin_button");
  GLADE_HOOKUP_OBJECT (options_dialog, button73, "button73");
  GLADE_HOOKUP_OBJECT (options_dialog, hbox12, "hbox12");
  GLADE_HOOKUP_OBJECT (options_dialog, button69, "button69");
  GLADE_HOOKUP_OBJECT (options_dialog, hseparator5, "hseparator5");
  GLADE_HOOKUP_OBJECT_NO_REF (options_dialog, tooltips, "tooltips");

  gtk_window_add_accel_group (GTK_WINDOW (options_dialog), accel_group);

  return options_dialog;
}

GtkWidget*
create_logging_dialog (void)
{
  GtkWidget *logging_dialog;
  GtkWidget *vbox14;
  GtkWidget *log_table;
  GtkWidget *hseparator6;
  GtkWidget *hbox13;
  GtkWidget *reset_all;
  GtkWidget *button83;
  GtkAccelGroup *accel_group;
  GtkTooltips *tooltips;

  tooltips = gtk_tooltips_new ();

  accel_group = gtk_accel_group_new ();

  logging_dialog = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  gtk_window_set_title (GTK_WINDOW (logging_dialog), _("Logging Configuration"));
  gtk_window_set_default_size (GTK_WINDOW (logging_dialog), 400, 400);

  vbox14 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox14);
  gtk_container_add (GTK_CONTAINER (logging_dialog), vbox14);

  log_table = gtk_table_new (1, 1, FALSE);
  gtk_widget_show (log_table);
  gtk_box_pack_start (GTK_BOX (vbox14), log_table, TRUE, TRUE, 0);

  hseparator6 = gtk_hseparator_new ();
  gtk_widget_show (hseparator6);
  gtk_box_pack_start (GTK_BOX (vbox14), hseparator6, FALSE, TRUE, 0);

  hbox13 = gtk_hbox_new (TRUE, 0);
  gtk_widget_show (hbox13);
  gtk_box_pack_start (GTK_BOX (vbox14), hbox13, FALSE, TRUE, 0);

  reset_all = gtk_button_new_with_mnemonic (_("Reset All"));
  gtk_widget_show (reset_all);
  gtk_box_pack_start (GTK_BOX (hbox13), reset_all, FALSE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (reset_all), 4);

  button83 = gtk_button_new_with_mnemonic (_("Close"));
  gtk_widget_show (button83);
  gtk_box_pack_start (GTK_BOX (hbox13), button83, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button83), 4);
  gtk_tooltips_set_tip (tooltips, button83, _("Close window and continue emulation"), NULL);
  gtk_widget_add_accelerator (button83, "clicked", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);

  g_signal_connect ((gpointer) log_table, "realize",
                    G_CALLBACK (on_logging_log_table_realize),
                    NULL);
  g_signal_connect ((gpointer) reset_all, "clicked",
                    G_CALLBACK (on_logging_reset_all_clicked),
                    NULL);
  g_signal_connect ((gpointer) button83, "clicked",
                    G_CALLBACK (on_logging_dialog_close_button_clicked),
                    NULL);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (logging_dialog, logging_dialog, "logging_dialog");
  GLADE_HOOKUP_OBJECT (logging_dialog, vbox14, "vbox14");
  GLADE_HOOKUP_OBJECT (logging_dialog, log_table, "log_table");
  GLADE_HOOKUP_OBJECT (logging_dialog, hseparator6, "hseparator6");
  GLADE_HOOKUP_OBJECT (logging_dialog, hbox13, "hbox13");
  GLADE_HOOKUP_OBJECT (logging_dialog, reset_all, "reset_all");
  GLADE_HOOKUP_OBJECT (logging_dialog, button83, "button83");
  GLADE_HOOKUP_OBJECT_NO_REF (logging_dialog, tooltips, "tooltips");

  gtk_window_add_accel_group (GTK_WINDOW (logging_dialog), accel_group);

  return logging_dialog;
}

GtkWidget*
create_fatal_dialog (void)
{
  GtkWidget *fatal_dialog;
  GtkWidget *dialog_vbox4;
  GtkWidget *message_label;
  GtkWidget *dialog_action_area4;
  GtkWidget *button84;

  fatal_dialog = gtk_dialog_new ();
  gtk_window_set_title (GTK_WINDOW (fatal_dialog), _("Fatal error"));
  gtk_window_set_modal (GTK_WINDOW (fatal_dialog), TRUE);

  dialog_vbox4 = GTK_DIALOG (fatal_dialog)->vbox;
  gtk_widget_show (dialog_vbox4);

  message_label = gtk_label_new ("");
  gtk_widget_show (message_label);
  gtk_box_pack_start (GTK_BOX (dialog_vbox4), message_label, FALSE, FALSE, 0);
  gtk_label_set_justify (GTK_LABEL (message_label), GTK_JUSTIFY_CENTER);
  gtk_misc_set_padding (GTK_MISC (message_label), 8, 8);

  dialog_action_area4 = GTK_DIALOG (fatal_dialog)->action_area;
  gtk_widget_show (dialog_action_area4);
  gtk_button_box_set_layout (GTK_BUTTON_BOX (dialog_action_area4), GTK_BUTTONBOX_END);

  button84 = gtk_button_new_with_mnemonic (_("OK"));
  gtk_widget_show (button84);
  gtk_dialog_add_action_widget (GTK_DIALOG (fatal_dialog), button84, 0);

  g_signal_connect ((gpointer) button84, "clicked",
                    G_CALLBACK (on_fatal_dialog_ok_button_clicked),
                    NULL);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (fatal_dialog, fatal_dialog, "fatal_dialog");
  GLADE_HOOKUP_OBJECT_NO_REF (fatal_dialog, dialog_vbox4, "dialog_vbox4");
  GLADE_HOOKUP_OBJECT (fatal_dialog, message_label, "message_label");
  GLADE_HOOKUP_OBJECT_NO_REF (fatal_dialog, dialog_action_area4, "dialog_action_area4");
  GLADE_HOOKUP_OBJECT (fatal_dialog, button84, "button84");

  return fatal_dialog;
}

GtkWidget*
create_completion_popup (void)
{
  GtkWidget *completion_popup;
  GtkWidget *completions;
  GtkAccelGroup *accel_group;

  accel_group = gtk_accel_group_new ();

  completion_popup = gtk_window_new (GTK_WINDOW_POPUP);
  gtk_widget_set_events (completion_popup, GDK_KEY_PRESS_MASK);
  gtk_widget_add_accelerator (completion_popup, "destroy", accel_group,
                              GDK_Escape, 0,
                              GTK_ACCEL_VISIBLE);
  gtk_window_set_modal (GTK_WINDOW (completion_popup), TRUE);

  completions = gtk_tree_view_new ();
  gtk_widget_show (completions);
  gtk_container_add (GTK_CONTAINER (completion_popup), completions);
  gtk_widget_set_events (completions, GDK_BUTTON_PRESS_MASK | GDK_BUTTON_RELEASE_MASK | GDK_KEY_PRESS_MASK);

  g_signal_connect_after ((gpointer) completions, "event",
                          G_CALLBACK (on_completions_event),
                          NULL);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (completion_popup, completion_popup, "completion_popup");
  GLADE_HOOKUP_OBJECT (completion_popup, completions, "completions");

  gtk_widget_grab_focus (completions);
  gtk_window_add_accel_group (GTK_WINDOW (completion_popup), accel_group);

  return completion_popup;
}

GtkWidget*
create_memory_map_dialog (void)
{
  GtkWidget *memory_map_dialog;
  GtkWidget *vbox16;
  GtkWidget *notebook;
  GtkWidget *empty_notebook_page;
  GtkWidget *label44;
  GtkWidget *label45;
  GtkWidget *label46;
  GtkWidget *label47;
  GtkWidget *hseparator7;
  GtkWidget *hbox16;
  GtkWidget *button94;
  GtkWidget *button93;

  memory_map_dialog = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  gtk_window_set_title (GTK_WINDOW (memory_map_dialog), _("Memory Map"));

  vbox16 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox16);
  gtk_container_add (GTK_CONTAINER (memory_map_dialog), vbox16);

  notebook = gtk_notebook_new ();
  gtk_widget_show (notebook);
  gtk_box_pack_start (GTK_BOX (vbox16), notebook, TRUE, TRUE, 0);

  empty_notebook_page = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (empty_notebook_page);
  gtk_container_add (GTK_CONTAINER (notebook), empty_notebook_page);

  label44 = gtk_label_new (_("Console / CPU"));
  gtk_widget_show (label44);
  gtk_notebook_set_tab_label (GTK_NOTEBOOK (notebook), gtk_notebook_get_nth_page (GTK_NOTEBOOK (notebook), 0), label44);
  gtk_label_set_justify (GTK_LABEL (label44), GTK_JUSTIFY_CENTER);

  empty_notebook_page = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (empty_notebook_page);
  gtk_container_add (GTK_CONTAINER (notebook), empty_notebook_page);

  label45 = gtk_label_new (_("GROM/GRAM"));
  gtk_widget_show (label45);
  gtk_notebook_set_tab_label (GTK_NOTEBOOK (notebook), gtk_notebook_get_nth_page (GTK_NOTEBOOK (notebook), 1), label45);
  gtk_label_set_justify (GTK_LABEL (label45), GTK_JUSTIFY_CENTER);

  empty_notebook_page = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (empty_notebook_page);
  gtk_container_add (GTK_CONTAINER (notebook), empty_notebook_page);

  label46 = gtk_label_new (_("VDP"));
  gtk_widget_show (label46);
  gtk_notebook_set_tab_label (GTK_NOTEBOOK (notebook), gtk_notebook_get_nth_page (GTK_NOTEBOOK (notebook), 2), label46);
  gtk_label_set_justify (GTK_LABEL (label46), GTK_JUSTIFY_CENTER);

  empty_notebook_page = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (empty_notebook_page);
  gtk_container_add (GTK_CONTAINER (notebook), empty_notebook_page);

  label47 = gtk_label_new (_("Speech"));
  gtk_widget_show (label47);
  gtk_notebook_set_tab_label (GTK_NOTEBOOK (notebook), gtk_notebook_get_nth_page (GTK_NOTEBOOK (notebook), 3), label47);
  gtk_label_set_justify (GTK_LABEL (label47), GTK_JUSTIFY_CENTER);

  hseparator7 = gtk_hseparator_new ();
  gtk_widget_show (hseparator7);
  gtk_box_pack_start (GTK_BOX (vbox16), hseparator7, FALSE, FALSE, 4);

  hbox16 = gtk_hbox_new (FALSE, 0);
  gtk_widget_show (hbox16);
  gtk_box_pack_start (GTK_BOX (vbox16), hbox16, FALSE, TRUE, 4);

  button94 = gtk_button_new_with_mnemonic (_("Refresh"));
  gtk_widget_show (button94);
  gtk_box_pack_start (GTK_BOX (hbox16), button94, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button94), 4);

  button93 = gtk_button_new_with_mnemonic (_("Close"));
  gtk_widget_show (button93);
  gtk_box_pack_start (GTK_BOX (hbox16), button93, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (button93), 4);

  g_signal_connect ((gpointer) memory_map_dialog, "realize",
                    G_CALLBACK (on_memory_map_dialog_realize),
                    NULL);
  g_signal_connect ((gpointer) memory_map_dialog, "destroy",
                    G_CALLBACK (on_memory_map_dialog_destroy),
                    NULL);
  g_signal_connect ((gpointer) button94, "clicked",
                    G_CALLBACK (on_memory_map_dialog_refresh_clicked),
                    NULL);
  g_signal_connect_swapped ((gpointer) button93, "clicked",
                            G_CALLBACK (gtk_widget_destroy),
                            GTK_OBJECT (memory_map_dialog));

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (memory_map_dialog, memory_map_dialog, "memory_map_dialog");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, vbox16, "vbox16");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, notebook, "notebook");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, label44, "label44");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, label45, "label45");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, label46, "label46");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, label47, "label47");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, hseparator7, "hseparator7");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, hbox16, "hbox16");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, button94, "button94");
  GLADE_HOOKUP_OBJECT (memory_map_dialog, button93, "button93");

  return memory_map_dialog;
}

static GnomeUIInfo menu1_uiinfo[] =
{
  {
    GNOME_APP_UI_ITEM, N_("_Flush Log"),
    NULL,
    (gpointer) on_flush_item_activate, NULL, NULL,
    GNOME_APP_PIXMAP_NONE, NULL,
    0, (GdkModifierType) 0, NULL
  },
  {
    GNOME_APP_UI_ITEM, N_("_Select Font..."),
    NULL,
    (gpointer) on_font_item_activate, NULL, NULL,
    GNOME_APP_PIXMAP_NONE, NULL,
    0, (GdkModifierType) 0, NULL
  },
  GNOMEUIINFO_END
};

static GnomeUIInfo menubar1_uiinfo[] =
{
  {
    GNOME_APP_UI_SUBTREE, N_("_Options"),
    NULL,
    menu1_uiinfo, NULL, NULL,
    GNOME_APP_PIXMAP_NONE, NULL,
    0, (GdkModifierType) 0, NULL
  },
  GNOMEUIINFO_END
};

GtkWidget*
create_command_dialog (void)
{
  GtkWidget *command_dialog;
  GtkWidget *hbox17;
  GtkWidget *vbox6;
  GtkWidget *menubar1;
  GtkWidget *log_text_window;
  GtkWidget *log_text_box;
  GtkWidget *command_text_entry;
  GtkWidget *progress_label;
  GtkWidget *v9t9_button_vbox;
  GtkWidget *reboot_button;
  GtkWidget *hseparator3;
  GtkWidget *pause_button;
  GtkWidget *hseparator1;
  GtkWidget *quick_save_button;
  GtkWidget *quick_load_button;
  GtkWidget *hseparator2;
  GtkWidget *button60;
  GtkWidget *button16;
  GtkWidget *module_button;
  GtkWidget *disks_button;
  GtkWidget *button37;
  GtkWidget *quit_button;
  GtkWidget *button74;
  GtkWidget *frame12;
  GtkWidget *hbox11;
  GtkWidget *disk_access_drawing_area;
  GtkWidget *rs232_access_drawing_area;
  GtkTooltips *tooltips;

  tooltips = gtk_tooltips_new ();

  command_dialog = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  gtk_window_set_title (GTK_WINDOW (command_dialog), _("Command Central"));

  gtk_widget_set_size_request(command_dialog, 600, 400);

  hbox17 = gtk_hbox_new (FALSE, 0);
  gtk_widget_show (hbox17);
  gtk_container_add (GTK_CONTAINER (command_dialog), hbox17);

  vbox6 = gtk_vbox_new (FALSE, 0);
  gtk_widget_show (vbox6);
  gtk_box_pack_start (GTK_BOX (hbox17), vbox6, TRUE, TRUE, 0);

  menubar1 = gtk_menu_bar_new ();
  gtk_widget_show (menubar1);
  gtk_box_pack_start (GTK_BOX (vbox6), menubar1, FALSE, FALSE, 0);
  gnome_app_fill_menu (GTK_MENU_SHELL (menubar1), menubar1_uiinfo,
                       NULL, FALSE, 0);

  log_text_window = gtk_scrolled_window_new (NULL, NULL);
  gtk_widget_show (log_text_window);
  gtk_box_pack_start (GTK_BOX (vbox6), log_text_window, TRUE, TRUE, 0);
  GTK_WIDGET_UNSET_FLAGS (log_text_window, GTK_CAN_FOCUS);
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (log_text_window), GTK_POLICY_NEVER, GTK_POLICY_ALWAYS);
  gtk_scrolled_window_set_shadow_type (GTK_SCROLLED_WINDOW (log_text_window), GTK_SHADOW_IN);

  log_text_box = gtk_text_view_new ();
  gtk_widget_show (log_text_box);
  gtk_container_add (GTK_CONTAINER (log_text_window), log_text_box);
  GTK_WIDGET_UNSET_FLAGS (log_text_box, GTK_CAN_FOCUS);
  gtk_text_view_set_editable (GTK_TEXT_VIEW (log_text_box), FALSE);
  gtk_text_view_set_wrap_mode (GTK_TEXT_VIEW (log_text_box), GTK_WRAP_WORD);
  gtk_text_buffer_set_text (gtk_text_view_get_buffer (GTK_TEXT_VIEW (log_text_box)),
	_("\n"), -1);

  command_text_entry = gtk_entry_new ();
  gtk_widget_show (command_text_entry);
  gtk_box_pack_start (GTK_BOX (vbox6), command_text_entry, FALSE, FALSE, 0);
  GTK_WIDGET_SET_FLAGS (command_text_entry, GTK_CAN_DEFAULT);

  progress_label = gtk_label_new (_("Welcome to V9t9!"));
  gtk_widget_show (progress_label);
  gtk_box_pack_start (GTK_BOX (vbox6), progress_label, FALSE, FALSE, 2);
  gtk_misc_set_alignment (GTK_MISC (progress_label), 0, 0.5);
  gtk_misc_set_padding (GTK_MISC (progress_label), 4, 0);

  v9t9_button_vbox = gtk_vbox_new (FALSE, 2);
  gtk_widget_show (v9t9_button_vbox);
  gtk_box_pack_start (GTK_BOX (hbox17), v9t9_button_vbox, TRUE, TRUE, 0);
  gtk_container_set_border_width (GTK_CONTAINER (v9t9_button_vbox), 4);

  reboot_button = gtk_button_new_with_mnemonic (_("Reboot"));
  gtk_widget_show (reboot_button);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), reboot_button, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (reboot_button, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, reboot_button, _("Reset emulated 99/4A"), NULL);

  hseparator3 = gtk_hseparator_new ();
  gtk_widget_show (hseparator3);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), hseparator3, FALSE, FALSE, 2);

  pause_button = gtk_button_new_with_mnemonic (_("Pause"));
  gtk_widget_show (pause_button);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), pause_button, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (pause_button, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, pause_button, _("Pause execution of V9t9"), NULL);

  hseparator1 = gtk_hseparator_new ();
  gtk_widget_show (hseparator1);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), hseparator1, FALSE, FALSE, 2);

  quick_save_button = gtk_button_new_with_mnemonic (_("Save..."));
  gtk_widget_show (quick_save_button);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), quick_save_button, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (quick_save_button, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, quick_save_button, _("Save machine state"), NULL);

  quick_load_button = gtk_button_new_with_mnemonic (_("Load..."));
  gtk_widget_show (quick_load_button);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), quick_load_button, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (quick_load_button, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, quick_load_button, _("Load machine state"), NULL);

  hseparator2 = gtk_hseparator_new ();
  gtk_widget_show (hseparator2);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), hseparator2, FALSE, FALSE, 2);

  button60 = gtk_button_new_with_mnemonic (_("Options..."));
  gtk_widget_show (button60);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), button60, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (button60, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, button60, _("Set basic configuration options"), NULL);

  button16 = gtk_button_new_with_mnemonic (_("Debugger..."));
  gtk_widget_show (button16);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), button16, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (button16, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, button16, _("Control emulated 99/4A and trace execution"), NULL);

  module_button = gtk_button_new_with_mnemonic (_("Modules..."));
  gtk_widget_show (module_button);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), module_button, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (module_button, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, module_button, _("Load or unload modules"), NULL);

  disks_button = gtk_button_new_with_mnemonic (_("Disks..."));
  gtk_widget_show (disks_button);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), disks_button, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (disks_button, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, disks_button, _("Assign drives to local directories or files"), NULL);

  button37 = gtk_button_new_with_mnemonic (_("Memory..."));
  gtk_widget_show (button37);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), button37, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (button37, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, button37, _("Assign files to ROM or RAM segments of the memory map"), NULL);

  quit_button = gtk_button_new_with_mnemonic (_("Quit"));
  gtk_widget_show (quit_button);
  gtk_box_pack_end (GTK_BOX (v9t9_button_vbox), quit_button, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (quit_button, GTK_CAN_FOCUS);

  button74 = gtk_button_new_with_mnemonic (_("Logging..."));
  gtk_widget_show (button74);
  gtk_box_pack_start (GTK_BOX (v9t9_button_vbox), button74, FALSE, FALSE, 0);
  GTK_WIDGET_UNSET_FLAGS (button74, GTK_CAN_FOCUS);
  gtk_tooltips_set_tip (tooltips, button74, _("Modify log levels for debugging"), NULL);

  frame12 = gtk_frame_new (NULL);
  gtk_widget_show (frame12);
  gtk_box_pack_end (GTK_BOX (v9t9_button_vbox), frame12, FALSE, FALSE, 0);

  hbox11 = gtk_hbox_new (FALSE, 0);
  gtk_widget_show (hbox11);
  gtk_container_add (GTK_CONTAINER (frame12), hbox11);

  disk_access_drawing_area = gtk_drawing_area_new ();
  gtk_widget_show (disk_access_drawing_area);
  gtk_box_pack_start (GTK_BOX (hbox11), disk_access_drawing_area, TRUE, TRUE, 2);

  rs232_access_drawing_area = gtk_drawing_area_new ();
  gtk_widget_show (rs232_access_drawing_area);
  gtk_box_pack_start (GTK_BOX (hbox11), rs232_access_drawing_area, TRUE, TRUE, 2);

  g_signal_connect ((gpointer) command_dialog, "key_press_event",
                    G_CALLBACK (on_command_dialog_key_press_event),
                    NULL);
  g_signal_connect ((gpointer) command_dialog, "destroy",
                    G_CALLBACK (on_command_dialog_destroy),
                    NULL);
  g_signal_connect ((gpointer) log_text_box, "realize",
                    G_CALLBACK (on_log_text_box_realize_event),
                    NULL);
  g_signal_connect ((gpointer) log_text_box, "button_press_event",
                    G_CALLBACK (on_command_dialog_button_press_event),
                    NULL);
  g_signal_connect ((gpointer) command_text_entry, "activate",
                    G_CALLBACK (on_command_text_entry_activate),
                    NULL);
  g_signal_connect_after ((gpointer) command_text_entry, "key_press_event",
                          G_CALLBACK (on_command_text_entry_key_press_event),
                          NULL);
  g_signal_connect ((gpointer) reboot_button, "clicked",
                    G_CALLBACK (on_v9t9_button_clicked),
                    "ResetComputer");
  g_signal_connect ((gpointer) pause_button, "clicked",
                    G_CALLBACK (on_v9t9_pause_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) quick_save_button, "clicked",
                    G_CALLBACK (on_v9t9_quick_load_save_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) quick_load_button, "clicked",
                    G_CALLBACK (on_v9t9_quick_load_save_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button60, "clicked",
                    G_CALLBACK (on_v9t9_window_options_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button16, "clicked",
                    G_CALLBACK (on_v9t9_debug_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) module_button, "clicked",
                    G_CALLBACK (on_v9t9_window_module_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) disks_button, "clicked",
                    G_CALLBACK (on_v9t9_window_disks_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button37, "clicked",
                    G_CALLBACK (on_v9t9_window_memory_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) quit_button, "clicked",
                    G_CALLBACK (on_quit_button_clicked),
                    NULL);
  g_signal_connect ((gpointer) button74, "clicked",
                    G_CALLBACK (on_v9t9_window_logging_button_clicked),
                    NULL);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (command_dialog, command_dialog, "command_dialog");
  GLADE_HOOKUP_OBJECT (command_dialog, hbox17, "hbox17");
  GLADE_HOOKUP_OBJECT (command_dialog, vbox6, "vbox6");
  GLADE_HOOKUP_OBJECT (command_dialog, menubar1, "menubar1");
  GLADE_HOOKUP_OBJECT (command_dialog, menubar1_uiinfo[0].widget, "menuitem2");
  GLADE_HOOKUP_OBJECT (command_dialog, menu1_uiinfo[0].widget, "menuitem3");
  GLADE_HOOKUP_OBJECT (command_dialog, menu1_uiinfo[1].widget, "menuitem4");
  GLADE_HOOKUP_OBJECT (command_dialog, log_text_window, "log_text_window");
  GLADE_HOOKUP_OBJECT (command_dialog, log_text_box, "log_text_box");
  GLADE_HOOKUP_OBJECT (command_dialog, command_text_entry, "command_text_entry");
  GLADE_HOOKUP_OBJECT (command_dialog, progress_label, "progress_label");
  GLADE_HOOKUP_OBJECT (command_dialog, v9t9_button_vbox, "v9t9_button_vbox");
  GLADE_HOOKUP_OBJECT (command_dialog, reboot_button, "reboot_button");
  GLADE_HOOKUP_OBJECT (command_dialog, hseparator3, "hseparator3");
  GLADE_HOOKUP_OBJECT (command_dialog, pause_button, "pause_button");
  GLADE_HOOKUP_OBJECT (command_dialog, hseparator1, "hseparator1");
  GLADE_HOOKUP_OBJECT (command_dialog, quick_save_button, "quick_save_button");
  GLADE_HOOKUP_OBJECT (command_dialog, quick_load_button, "quick_load_button");
  GLADE_HOOKUP_OBJECT (command_dialog, hseparator2, "hseparator2");
  GLADE_HOOKUP_OBJECT (command_dialog, button60, "button60");
  GLADE_HOOKUP_OBJECT (command_dialog, button16, "button16");
  GLADE_HOOKUP_OBJECT (command_dialog, module_button, "module_button");
  GLADE_HOOKUP_OBJECT (command_dialog, disks_button, "disks_button");
  GLADE_HOOKUP_OBJECT (command_dialog, button37, "button37");
  GLADE_HOOKUP_OBJECT (command_dialog, quit_button, "quit_button");
  GLADE_HOOKUP_OBJECT (command_dialog, button74, "button74");
  GLADE_HOOKUP_OBJECT (command_dialog, frame12, "frame12");
  GLADE_HOOKUP_OBJECT (command_dialog, hbox11, "hbox11");
  GLADE_HOOKUP_OBJECT (command_dialog, disk_access_drawing_area, "disk_access_drawing_area");
  GLADE_HOOKUP_OBJECT (command_dialog, rs232_access_drawing_area, "rs232_access_drawing_area");
  GLADE_HOOKUP_OBJECT_NO_REF (command_dialog, tooltips, "tooltips");

  gtk_widget_grab_focus (command_text_entry);
  gtk_widget_grab_default (command_text_entry);
  return command_dialog;
}

static GnomeUIInfo file1_menu_uiinfo[] =
{
  GNOMEUIINFO_MENU_NEW_ITEM (N_("_New Session"), NULL, on_newsession1_activate, NULL),
  GNOMEUIINFO_MENU_OPEN_ITEM (on_open1_activate, NULL),
  GNOMEUIINFO_MENU_SAVE_ITEM (on_save1_activate, NULL),
  GNOMEUIINFO_MENU_SAVE_AS_ITEM (on_save_as1_activate, NULL),
  GNOMEUIINFO_SEPARATOR,
  {
    GNOME_APP_UI_ITEM, N_("_Load Configuration"),
    NULL,
    (gpointer) on_load_configuration1_activate, NULL, NULL,
    GNOME_APP_PIXMAP_NONE, NULL,
    0, (GdkModifierType) 0, NULL
  },
  {
    GNOME_APP_UI_ITEM, N_("Sa_ve Configuration"),
    NULL,
    (gpointer) on_save_configuration1_activate, NULL, NULL,
    GNOME_APP_PIXMAP_NONE, NULL,
    0, (GdkModifierType) 0, NULL
  },
  {
    GNOME_APP_UI_ITEM, N_("Save _Configuration As..."),
    NULL,
    (gpointer) on_save_configuration_as1_activate, NULL, NULL,
    GNOME_APP_PIXMAP_NONE, NULL,
    0, (GdkModifierType) 0, NULL
  },
  GNOMEUIINFO_SEPARATOR,
  GNOMEUIINFO_MENU_EXIT_ITEM (on_quit1_activate, NULL),
  GNOMEUIINFO_END
};

static GnomeUIInfo edit1_menu_uiinfo[] =
{
  GNOMEUIINFO_MENU_CUT_ITEM (on_cut1_activate, NULL),
  GNOMEUIINFO_MENU_COPY_ITEM (on_copy1_activate, NULL),
  GNOMEUIINFO_MENU_PASTE_ITEM (on_paste1_activate, NULL),
  GNOMEUIINFO_MENU_CLEAR_ITEM (on_clear1_activate, NULL),
  GNOMEUIINFO_SEPARATOR,
  GNOMEUIINFO_MENU_PROPERTIES_ITEM (on_properties1_activate, NULL),
  GNOMEUIINFO_SEPARATOR,
  GNOMEUIINFO_MENU_PREFERENCES_ITEM (on_preferences1_activate, NULL),
  GNOMEUIINFO_END
};

static GnomeUIInfo view1_uiinfo[] =
{
  GNOMEUIINFO_END
};

static GnomeUIInfo help1_menu_uiinfo[] =
{
  GNOMEUIINFO_HELP ("v9t9"),
  GNOMEUIINFO_MENU_ABOUT_ITEM (on_about1_activate, NULL),
  GNOMEUIINFO_END
};

static GnomeUIInfo menubar2_uiinfo[] =
{
  GNOMEUIINFO_MENU_FILE_TREE (file1_menu_uiinfo),
  GNOMEUIINFO_MENU_EDIT_TREE (edit1_menu_uiinfo),
  GNOMEUIINFO_MENU_VIEW_TREE (view1_uiinfo),
  GNOMEUIINFO_MENU_HELP_TREE (help1_menu_uiinfo),
  GNOMEUIINFO_END
};

GtkWidget*
create_app1 (void)
{
  GtkWidget *app1;
  GtkWidget *bonobodock1;
  GtkWidget *toolbar1;
  GtkWidget *button98;
  GtkWidget *button99;
  GtkWidget *button100;
  GtkWidget *appbar1;

  app1 = gnome_app_new ("V9t9", _("V9t9"));

  bonobodock1 = GNOME_APP (app1)->dock;
  gtk_widget_show (bonobodock1);

  gnome_app_create_menus (GNOME_APP (app1), menubar2_uiinfo);

  toolbar1 = gtk_toolbar_new ();
  gtk_widget_show (toolbar1);
  gnome_app_add_toolbar (GNOME_APP (app1), GTK_TOOLBAR (toolbar1), "toolbar1",
                                BONOBO_DOCK_ITEM_BEH_EXCLUSIVE,
                                BONOBO_DOCK_TOP, 1, 0, 0);
  gtk_toolbar_set_style (GTK_TOOLBAR (toolbar1), GTK_TOOLBAR_BOTH);

  button98 = gtk_toolbar_insert_stock (GTK_TOOLBAR (toolbar1),
                                "gtk-new",
                                _("New File"),
                                NULL, NULL, NULL, -1);
  gtk_widget_show (button98);

  button99 = gtk_toolbar_insert_stock (GTK_TOOLBAR (toolbar1),
                                "gtk-open",
                                _("Open File"),
                                NULL, NULL, NULL, -1);
  gtk_widget_show (button99);

  button100 = gtk_toolbar_insert_stock (GTK_TOOLBAR (toolbar1),
                                "gtk-save",
                                _("Save File"),
                                NULL, NULL, NULL, -1);
  gtk_widget_show (button100);

  appbar1 = gnome_appbar_new (TRUE, TRUE, GNOME_PREFERENCES_NEVER);
  gtk_widget_show (appbar1);
  gnome_app_set_statusbar (GNOME_APP (app1), appbar1);

  gnome_app_install_menu_hints (GNOME_APP (app1), menubar2_uiinfo);

  /* Store pointers to all widgets, for use by lookup_widget(). */
  GLADE_HOOKUP_OBJECT_NO_REF (app1, app1, "app1");
  GLADE_HOOKUP_OBJECT (app1, bonobodock1, "bonobodock1");
  GLADE_HOOKUP_OBJECT (app1, menubar2_uiinfo[0].widget, "file1");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[0].widget, "newsession1");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[1].widget, "open1");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[2].widget, "save1");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[3].widget, "save_as1");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[4].widget, "separator1");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[5].widget, "load_configuration1");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[6].widget, "save_configuration1");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[7].widget, "save_configuration_as1");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[8].widget, "separator4");
  GLADE_HOOKUP_OBJECT (app1, file1_menu_uiinfo[9].widget, "quit1");
  GLADE_HOOKUP_OBJECT (app1, menubar2_uiinfo[1].widget, "edit1");
  GLADE_HOOKUP_OBJECT (app1, edit1_menu_uiinfo[0].widget, "cut1");
  GLADE_HOOKUP_OBJECT (app1, edit1_menu_uiinfo[1].widget, "copy1");
  GLADE_HOOKUP_OBJECT (app1, edit1_menu_uiinfo[2].widget, "paste1");
  GLADE_HOOKUP_OBJECT (app1, edit1_menu_uiinfo[3].widget, "clear1");
  GLADE_HOOKUP_OBJECT (app1, edit1_menu_uiinfo[4].widget, "separator2");
  GLADE_HOOKUP_OBJECT (app1, edit1_menu_uiinfo[5].widget, "properties1");
  GLADE_HOOKUP_OBJECT (app1, edit1_menu_uiinfo[6].widget, "separator3");
  GLADE_HOOKUP_OBJECT (app1, edit1_menu_uiinfo[7].widget, "preferences1");
  GLADE_HOOKUP_OBJECT (app1, menubar2_uiinfo[2].widget, "view1");
  GLADE_HOOKUP_OBJECT (app1, menubar2_uiinfo[3].widget, "help1");
  GLADE_HOOKUP_OBJECT (app1, help1_menu_uiinfo[1].widget, "about1");
  GLADE_HOOKUP_OBJECT (app1, toolbar1, "toolbar1");
  GLADE_HOOKUP_OBJECT (app1, button98, "button98");
  GLADE_HOOKUP_OBJECT (app1, button99, "button99");
  GLADE_HOOKUP_OBJECT (app1, button100, "button100");
  GLADE_HOOKUP_OBJECT (app1, appbar1, "appbar1");

  return app1;
}

