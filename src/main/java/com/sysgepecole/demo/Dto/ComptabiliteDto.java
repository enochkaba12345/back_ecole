package com.sysgepecole.demo.Dto;

import java.util.Date;

public class ComptabiliteDto {
	
	private Long id;
    private String libelle;
    private Double montant;
    private Double montants;
    private Boolean annule;
    private String typeoperation; 
    private String etape; 
    private Date dateOperation;
    private Date dateOperationmodi;
    private Long iduser;
    private String nom;
    private String postnom;
    private String prenom;
    private Long idusermodi;
    private Long idannee;
    private String annee;
    private String username;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getLibelle() {
		return libelle;
	}
	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}
	public Double getMontant() {
		return montant;
	}
	public void setMontant(Double montant) {
		this.montant = montant;
	}
	public Double getMontants() {
		return montants;
	}
	public void setMontants(Double montants) {
		this.montants = montants;
	}
	public Boolean getAnnule() {
		return annule;
	}
	public void setAnnule(Boolean annule) {
		this.annule = annule;
	}
	public String getTypeoperation() {
		return typeoperation;
	}
	public void setTypeoperation(String typeoperation) {
		this.typeoperation = typeoperation;
	}
	public String getEtape() {
		return etape;
	}
	public void setEtape(String etape) {
		this.etape = etape;
	}
	public Date getDateOperation() {
		return dateOperation;
	}
	public void setDateOperation(Date dateOperation) {
		this.dateOperation = dateOperation;
	}
	public Date getDateOperationmodi() {
		return dateOperationmodi;
	}
	public void setDateOperationmodi(Date dateOperationmodi) {
		this.dateOperationmodi = dateOperationmodi;
	}
	public Long getIduser() {
		return iduser;
	}
	public void setIduser(Long iduser) {
		this.iduser = iduser;
	}
	public Long getIdusermodi() {
		return idusermodi;
	}
	public void setIdusermodi(Long idusermodi) {
		this.idusermodi = idusermodi;
	}
	public Long getIdannee() {
		return idannee;
	}
	public void setIdannee(Long idannee) {
		this.idannee = idannee;
	}
	
	
	public String getNom() {
		return nom;
	}
	public void setNom(String nom) {
		this.nom = nom;
	}
	public String getPostnom() {
		return postnom;
	}
	public void setPostnom(String postnom) {
		this.postnom = postnom;
	}
	public String getPrenom() {
		return prenom;
	}
	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}
	public String getAnnee() {
		return annee;
	}
	public void setAnnee(String annee) {
		this.annee = annee;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	@Override
	public String toString() {
		return "ComptabiliteDto [id=" + id + ", libelle=" + libelle + ", montant=" + montant + ", montants=" + montants
				+ ", annule=" + annule + ", typeoperation=" + typeoperation + ", etape=" + etape + ", dateOperation="
				+ dateOperation + ", dateOperationmodi=" + dateOperationmodi + ", iduser=" + iduser + ", nom=" + nom
				+ ", postnom=" + postnom + ", prenom=" + prenom + ", idusermodi=" + idusermodi + ", idannee=" + idannee
				+ ", annee=" + annee + ", username=" + username + "]";
	}
    
    
    

}
