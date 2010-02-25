package relay.position;

import parameters.ElectionConfiguration;
import relay.wnic.exception.InvalidParameter;
import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * Grey model per il filtraggio dei valori degli RSSI.
 * @author Zapparoli Pamela
 * @version 1.0
  */

public class GreyModel implements RSSIFilter
{
	/**
	 * Numero minimo di campioni di segnale necessari ad effettuare una predizione col Grey Model
	 */
	//public static final int NUMBER_OF_RSSI=5;
	//public static final double PREVISION_THRS=20;
	private double realValues[];
	private long predictedTime=-1;
	private int time=-1;
	private boolean computed=false;
	private double[] X1;
	private double a, u;
	
	//per i test
	private double PREVISION_THRS;
	private double GREY_MIN_NUMBER_OF_RSSI;

	/**
	 * Costruttore del Grey Model
	 * @param realValues array coi valori campionati del segnale
	 * @throws InvalidParameter se l'array dei segnali e' vuoto o se vi sono dei valori negativi
	 */
	public GreyModel(double [] realValues)throws InvalidParameter
	{
		setOriginalValues(realValues);
		setGREY_MIN_NUMBER_OF_RSSI(ElectionConfiguration.GREY_MIN_NUMBER_OF_RSSI);
		setPREVISION_THRS(ElectionConfiguration.PREVISION_THRS);
	}
	
	public GreyModel(double [] realValues, int min, double prev)throws InvalidParameter
	{
		setOriginalValues(realValues);
		setGREY_MIN_NUMBER_OF_RSSI(min);
		setPREVISION_THRS(prev);
	}
//+++++++++++++++++++++++++++++++++++++++++++++ROBA NUOVA+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**Modifica al predictRSSI fatta da Luca
	 * @return 
	 * @throws niente
	 */
	public double predictRSSI()
	{
		predictedTime=realValues.length;
	
		//se non vi sono abbastanza valori per fare la previsione restituisce il valore di RSSI attuale reale
		if(realValues.length<GREY_MIN_NUMBER_OF_RSSI){
			System.out.println("Non ci sono abbastanza valori per fare la predizione: "+(realValues.length)+" servono:"+GREY_MIN_NUMBER_OF_RSSI);
			return realValues[realValues.length-1];
		}
		
		try
		{
			if(!computed)
			{
				//calcola X1
				X1= new double[realValues.length];
				//X1[0]=realValues[0];
				for(int i=0;i<realValues.length;i++)
					if(i==0)
						X1[i]=realValues[i];
					else
						X1[i]=X1[i-1]+realValues[i];		
				//calcola B
				double[][] B= new double[realValues.length-1][2];
				for(int i=1;i<realValues.length;i++)
				{
					B[i-1][0]=-0.5*(X1[i-1]+X1[i]);
					B[i-1][1]=1;
				}
				//calcola y
				Vector yn;
				double[] tmp = new double[realValues.length-1];
				for(int i=0;i<(realValues.length-1);i++){
					tmp[i] = realValues[i+1];
				}
				yn = new Vector(tmp); //Vettore Yn
				Matrix bmatr= new Matrix(B);   //creo la matrice B
				Matrix bt= bmatr.transpose();	//B^T
				Matrix res= bt.times(bmatr);	//(B^T*B)
				Matrix inverse;
				inverse= res.inverse();		//(B^T*B)^-1
				res=inverse.times(bt);
				res= res.times(yn);
				double [][] r=res.getArrayCopy();
				a=r[0][0];
				u=r[1][0];
				computed=true;
			}

			//calcola la predizione
			double xk=(X1[0]-(u/a))*Math.exp(-a*predictedTime)+(u/a);
			double xk1=(X1[0]-(u/a))*Math.exp(-a*(predictedTime+1))+(u/a);

			//return xk1-xk;
			double res=xk1-xk;
			if(Double.isNaN(res))
				res= realValues[realValues.length-1];
			if(Math.abs(res-realValues[realValues.length-1])>=PREVISION_THRS)
				res= realValues[realValues.length-1];
			return res;

		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
			return realValues[realValues.length-1];
		}
	}
//++++++++++++++++++++++++++++++++++++ROBA NUOVA+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	

	/**
	 * Predice il valore che il segnale avra' fra time secondi 
	 * @param time l'istante in cui effettuare la predizione
	 * @param samplingTime tempo di campionamento del segnale originale
	 * @return il valore predetto del segnale
	 * @throws InvalidParameter se il tempo d predizione o di campionamento sono negativi
	 */
	public double predictRSSI(int time, long samplingTime) throws InvalidParameter
	{		
		if((time<=0) || (samplingTime<=0))
			throw new InvalidParameter("ERRORE: parametri della predizione non validi");
		if(realValues==null)
			throw new InvalidParameter("ERRORE: effettuare la setOriginalValues per settare i valori originali prima di effettuare la predizione");
		//tempo di predizione in unita' di misura del campionamento
		predictedTime=(time*1000/samplingTime)+realValues.length;
		this.time=time;

		//se non vi sono abbastanza valori per fare la previsione restituisce il valore di RSSI attuale reale
		if(realValues.length<ElectionConfiguration.GREY_MIN_NUMBER_OF_RSSI)
			return realValues[realValues.length-1];
		try
		{
			if(!computed)
			{
				//calcola X1
				X1= new double[realValues.length];
				X1[0]=realValues[0];
				for(int i=1;i<realValues.length;i++)
					X1[i]=X1[i-1]+realValues[i];		
				//calcola B
				double[][] B= new double[realValues.length-1][2];
				for(int i=1;i<realValues.length;i++)
				{
					B[i-1][0]=-0.5*(X1[i-1]+X1[i]);
					B[i-1][1]=1;
				}
				//calcola y
				double[][] y= new double[realValues.length-1][1];
				for(int i=0;i<(realValues.length-1);i++)
					y[i][0]=realValues[i+1];
				//calcola a e u
				Matrix bmatr= new Matrix(B);
				Matrix bt= bmatr.transpose();
				Matrix res= bt.times(bmatr);
				Matrix inverse;
				if(res.det()==0)
				{
					//m e' singolare, calcola la pseudoinversa con la singular value decomposition
					SingularValueDecomposition svd= new SingularValueDecomposition(res);
					//svd = U*S*V'
					Matrix invS=svd.getS().inverse();
					inverse=(svd.getV()).times(invS);
					inverse=inverse.times(svd.getU().transpose());
				}
				else
					inverse= res.inverse();
				Matrix ymatr= new Matrix(y);
				res=inverse.times(bt);
				res= res.times(ymatr);
				double [][] r=res.getArrayCopy();
				a=r[0][0];
				u=r[1][0];
				computed=true;
			}

			//System.out.println("------------> predictedTime: " + predictedTime);
			
			//calcola la predizione
			double xk=(X1[0]-(u/a))*Math.exp(-a*predictedTime)+(u/a);
			double xk1=(X1[0]-(u/a))*Math.exp(-a*(predictedTime+1))+(u/a);

			//return xk1-xk;
			double res=xk1-xk;
			if(res==Double.NaN)
				res= realValues[realValues.length-1];
			if(Math.abs(res-realValues[realValues.length-1])>=ElectionConfiguration.PREVISION_THRS)
				res= realValues[realValues.length-1];
			return res;

		}
		catch(Exception e)
		{
			return realValues[realValues.length-1];
		}
	}


	/**
	 * Indica il tempo della predizione in secondi a partire da ora
	 * @return l'istente della predizione in secondi o -1 se non e' ancora stata fatta la predizione
	 */
	public long predictTime() 
	{
		return time;
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
	
	public void setPREVISION_THRS(double p){this.PREVISION_THRS = p;}
	public void setGREY_MIN_NUMBER_OF_RSSI(int p){this.GREY_MIN_NUMBER_OF_RSSI = p;}
	
}
