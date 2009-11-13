package muse.client;


//classe che gestisce l'attesa dell'istante di startpoint e ne segnala il raggiugimento al listener invocando il suo metodo di avvio della riproduzione del flusso RTP
public class StartpointTimer
{
	private Thread runner;
	private double startpoint=0;
	private IStartpointListener spListener;

	public StartpointTimer(IStartpointListener listener, double sp)
	{
		startpoint=sp;
		spListener=listener;
		runner=new Thread(){ public void run(){ waitForStartpoint(); } };
		runner.setPriority(Thread.MAX_PRIORITY);
		runner.start();
	}

	private void waitForStartpoint()
	{
		long start = System.currentTimeMillis()+(long)startpoint;
		do
		{
			try{ Thread.sleep(20); }
			catch(InterruptedException e){}
		}
		while((start - System.currentTimeMillis())>0);
		System.out.println("Startpoint giunto!");
		if(spListener!=null) spListener.startStreamPlaying();
		runner.interrupt();
	}

	public void setStartpoint(double sp){ startpoint=sp; }
	public double getStartpoint(){ return startpoint; }
}