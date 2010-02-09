package relay;

class Couple implements Comparable<Couple> {

	private double weight = -1;
	private String address = null;

	public Couple(String address, double weight){
		this.address = address;
		this.weight = weight;		
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Couple c) {
		int res = 0;
		if(this.weight > c.weight)res=-1;
		else if(this.weight < c.weight) res = +1;
		else res = this.address.compareTo(c.address);
		return res;
	}

	public String toString(){
		return "Address: " + this.address 
		+ " Weight: " + this.weight;
	}

	public double getWeight() {
		return weight;
	}

	public String getAddress() {
		return address;
	}

}