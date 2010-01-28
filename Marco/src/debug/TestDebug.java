package debug;

public class TestDebug {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DebugConsole console = new DebugConsole();
		console.debugMessage(0,"INFO");
		console.debugMessage(1,"WARNING");
		console.debugMessage(2,"ERROR");
	}
}
