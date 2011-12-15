/**
 * 
 */
package v9t9.common.machine;

import v9t9.common.memory.IMemoryDomain;

/**
 * This interface encapsulates any aspect of the emulator
 * which uses registers.
 * @author ejs
 *
 */
public interface IRegisterAccess {
	
	int FLAG_VOLATILE = 1 << 3;
	int FLAG_ROLE_GENERAL = 0;
	int FLAG_ROLE_PC = 1;
	int FLAG_ROLE_ST = 2;
	int FLAG_ROLE_SP = 3;
	int FLAG_ROLE_FP = 4;
	int FLAG_ROLE_RET = 5;
	int FLAG_ROLE_MASK = 0x7;
	
	class RegisterInfo {
		public final String id;
		public final int flags;
		public final int size;
		public final String description;
		public IMemoryDomain domain;
		public int addr;
		
		public RegisterInfo(String id, int flags, int size,
				String description) {
			this.id = id;
			this.flags = flags;
			this.size = size;
			this.description = description;
		}
		
		
	}

	String getGroupName();
	int getFirstRegister();
	int getRegisterCount();
	RegisterInfo getRegisterInfo(int reg);
	int getRegister(int reg);
	int setRegister(int reg, int newValue);
	String getRegisterTooltip(int reg);
	
	int getRegisterNumber(String id);
	
	interface IRegisterWriteListener {
		void registerChanged(int reg, int value);
	}
	
	void addWriteListener(IRegisterWriteListener listener);
	void removeWriteListener(IRegisterWriteListener listener);

}