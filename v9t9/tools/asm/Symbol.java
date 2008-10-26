/**
 * 
 */
package v9t9.tools.asm;

/**
 * @author ejs
 *
 */
public class Symbol {

	private final String name;
	private int addr;
	private boolean defined;

	public Symbol(String name) {
		this.name = name;
	}

	public Symbol(String name, int addr) {
		this.name = name;
		this.addr = addr;
		this.defined = true;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Symbol other = (Symbol) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public void setAddr(int addr) {
		this.addr = addr;
		this.defined = true;
	}
	public boolean isDefined() {
		return defined;
	}

	public int getAddr() {
		return addr;
	}
	
	
}
