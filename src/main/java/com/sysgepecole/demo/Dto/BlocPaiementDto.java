package com.sysgepecole.demo.Dto;

import java.util.List;

public class BlocPaiementDto {
	
	private String classe;
    private double totalFrais;
    private double totalPaye;
    private double dette;
    private String statut; // "DETTE", "ACOMPTE", "SOLDER"

    private List<PaiementDetailDto> details;

	public String getClasse() {
		return classe;
	}

	public void setClasse(String classe) {
		this.classe = classe;
	}

	public double getTotalFrais() {
		return totalFrais;
	}

	public void setTotalFrais(double totalFrais) {
		this.totalFrais = totalFrais;
	}

	public double getTotalPaye() {
		return totalPaye;
	}

	public void setTotalPaye(double totalPaye) {
		this.totalPaye = totalPaye;
	}

	public double getDette() {
		return dette;
	}

	public void setDette(double dette) {
		this.dette = dette;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

	public List<PaiementDetailDto> getDetails() {
		return details;
	}

	public void setDetails(List<PaiementDetailDto> details) {
		this.details = details;
	}
    
    
    

}
