package org.ejs.coffee.core.winmm;
/**
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a>, <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class WAVEINCAPSA extends Structure<WAVEINCAPSA, WAVEINCAPSA.ByValue, WAVEINCAPSA.ByReference> {
	public short wMid;
	public short wPid;
	/// Conversion Error : UINT
	/// C type : CHAR[32]
	public com.sun.jna.Pointer szPname;
	public int dwFormats;
	public short wChannels;
	public short wReserved1;
	public WAVEINCAPSA() {
		super();
	}
	/// @param szPname C type : CHAR[32]
	public WAVEINCAPSA(short wMid, short wPid, com.sun.jna.Pointer szPname, int dwFormats, short wChannels, short wReserved1) {
		super();
		this.wMid = wMid;
		this.wPid = wPid;
		this.szPname = szPname;
		this.dwFormats = dwFormats;
		this.wChannels = wChannels;
		this.wReserved1 = wReserved1;
	}
	protected ByReference newByReference() { return new ByReference(); }
	protected ByValue newByValue() { return new ByValue(); }
	protected WAVEINCAPSA newInstance() { return new WAVEINCAPSA(); }
	public static WAVEINCAPSA[] newArray(int arrayLength) {
		return Structure.newArray(WAVEINCAPSA.class, arrayLength);
	}
	public static class ByReference extends WAVEINCAPSA implements Structure.ByReference {}
	public static class ByValue extends WAVEINCAPSA implements Structure.ByValue {}
}