package test;

import debug.DebugConsole;

public class TestDebug {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DebugConsole console = new DebugConsole("TEST");
		console.debugMessage(0,"INFO");
		console.debugMessage(1,"WARNING");
		console.debugMessage(2,"ERROR");
	}
}
