package com.sysgepecole.demo.Dto;

public class PaiementDetailDto {
	private String tranche;   // 1ERE TRANCHE, 2EME TRANCHE...
    private String categorie; // MINERVAL, FOURNITURES, etc.
    private double montantPaye;
    private String statut;
	public String getTranche() {
		return tranche;
	}
	public void setTranche(String tranche) {
		this.tranche = tranche;
	}
	public String getCategorie() {
		return categorie;
	}
	public void setCategorie(String categorie) {
		this.categorie = categorie;
	}
	public double getMontantPaye() {
		return montantPaye;
	}
	public void setMontantPaye(double montantPaye) {
		this.montantPaye = montantPaye;
	}
	public String getStatut() {
		return statut;
	}
	public void setStatut(String statut) {
		this.statut = statut;
	}
    
    
    

}
