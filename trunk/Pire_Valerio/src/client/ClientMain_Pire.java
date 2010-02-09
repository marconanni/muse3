package client;



public class ClientMain_Pire {
	public static void main(String[] args) {

	//System.setProperty("java.net.preferIPv4Stack", "true");
	
/*
 * TODO: UNA VOLTA PRONTI I SESSIONMANAGER E ELECTIONMANAGER,
 * DEVONO FINIRE TUTTI E 2 DENTRO LA CLASSE CLIENTCONTROLLER CHE NON FA ALTRO CHE FUNGERE DA PONTE TRA I 2
 * MANAGER
 */
	//System.out.println("Stack:"+ System.getProperty("java.net.preferIPv4Stack"));
		//SwingUtilities.invokeLater(new Runnable() {
			//public void run() {		
				
				//ClientFrame frame = new ClientFrame();
				//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				//frame.setVisible(true);
				
				new ClientController_Pire();//frame);
								
			//}
		//});
	
		

}
}