package relay.position;

import relay.wnic.exception.InvalidParameter;

public class RSSI_Gradient implements RSSIFilter {
	
	public static final int NUMBER_OF_RSSI=5;
	public static final double PREVISION_THRS=20;
	private double realValues[];
	private long predictedTime=-1;
	private int time=-1;
	private boolean computed=false;
	private double[] X1;
	private double a, u;

	
	public RSSI_Gradient(double [] realValues)throws InvalidParameter
	{
		setOriginalValues(realValues);
	}
	@Override
	public double predictRSSI(int timeSec, long samplingTime)
			throws InvalidParameter {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double predictRSSI() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long predictTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Imposta i valori originali del segnale
	 * @param RSSIRealValues i valori originali del segnale
	 * @throws InvalidParameter se l'elenco dei valori e' vuoto o contiene valori negativi
	 */
	public void setOriginalValues(double[] RSSIRealValues) throws InvalidParameter
	{
		if(RSSIRealValues.length==0)
			throw new InvalidParameter("ERRORE: l'elenco dei precedenti valori di RSSI e' vuoto");
		for(int i=0;i<RSSIRealValues.length;i++)
		{
			if(RSSIRealValues[i]<0)
				throw new InvalidParameter("ERRORE: valore di RSSI negativo");
		}
		this.realValues=RSSIRealValues;
	}

}
