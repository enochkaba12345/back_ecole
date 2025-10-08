package com.sysgepecole.demo.Dto;


import java.util.Date;

public class EleveModelDto {

	private Long ideleve;
	private String noms;
	public String getNoms() {
		return noms;
	}
	public void setNoms(String noms) {
		this.noms = noms;
	}

	private String type_classe;
	private String nom;
	private String postnom;
	private String prenom;
	private String sexe;
	private String adresse;
	private String nomtuteur;
	private String email;
	private String telephone;
	private String statut_classe;
	private String matricule;
	private String pourcentage;
	
	private Long iduser;
	private String username;
	
	private Date dateins;
	private Date datenaiss;
	private Date date_passation;
	    
    private Long idintermedaireclasse;
    private Long idintermedaireannee;
    
    private Long idannee;
    private String annee;
    
    private Long idclasse;
    private String classe;
    
    private Long idecole;
    private String ecole;
    private String avenue;
   

    
    public String getAvenue() {
		return avenue;
	}
	public void setAvenue(String avenue) {
		this.avenue = avenue;
	}


	private Long idcommune;
    private String commune;
        
    private Long idprovince;
    private String province;
    
    private Long id;
    private String photo;
    private String logos;
    

    
	public Long getIdeleve() {
		return ideleve;
	}
	public void setIdeleve(Long ideleve) {
		this.ideleve = ideleve;
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
	public String getSexe() {
		return sexe;
	}
	public void setSexe(String sexe) {
		this.sexe = sexe;
	}
	public String getAdresse() {
		return adresse;
	}
	public void setAdresse(String adresse) {
		this.adresse = adresse;
	}
	public String getNomtuteur() {
		return nomtuteur;
	}
	public void setNomtuteur(String nomtuteur) {
		this.nomtuteur = nomtuteur;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getTelephone() {
		return telephone;
	}
	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}
	public Date getDateins() {
		return dateins;
	}
	public void setDateins(Date dateins) {
		this.dateins = dateins;
	}
	public Date getDatenaiss() {
		return datenaiss;
	}
	public void setDatenaiss(Date datenaiss) {
		this.datenaiss = datenaiss;
	}
	public Long getIdintermedaireclasse() {
		return idintermedaireclasse;
	}
	public void setIdintermedaireclasse(Long idintermedaireclasse) {
		this.idintermedaireclasse = idintermedaireclasse;
	}
	public Long getIdintermedaireannee() {
		return idintermedaireannee;
	}
	public void setIdintermedaireannee(Long idintermedaireannee) {
		this.idintermedaireannee = idintermedaireannee;
	}
	public Long getIdannee() {
		return idannee;
	}
	public void setIdannee(Long idannee) {
		this.idannee = idannee;
	}
	public String getAnnee() {
		return annee;
	}
	public void setAnnee(String annee) {
		this.annee = annee;
	}
	public Long getIdclasse() {
		return idclasse;
	}
	public void setIdclasse(Long idclasse) {
		this.idclasse = idclasse;
	}
	public String getClasse() {
		return classe;
	}
	public void setClasse(String classe) {
		this.classe = classe;
	}
	public String getEcole() {
		return ecole;
	}
	public void setEcole(String ecole) {
		this.ecole = ecole;
	}
	public Long getIdecole() {
		return idecole;
	}
	public void setIdecole(Long idecole) {
		this.idecole = idecole;
	}
	public String getCommune() {
		return commune;
	}
	public void setCommune(String commune) {
		this.commune = commune;
	}
	public Long getIdcommune() {
		return idcommune;
	}
	public void setIdcommune(Long idcommune) {
		this.idcommune = idcommune;
	}
	public String getProvince() {
		return province;
	}
	public void setProvince(String province) {
		this.province = province;
	}
	public Long getIdprovince() {
		return idprovince;
	}
	public void setIdprovince(Long idprovince) {
		this.idprovince = idprovince;
	}
    
    
public Long getIduser() {
		return iduser;
	}
	public void setIduser(Long iduser) {
		this.iduser = iduser;
	}
	
	
public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
public EleveModelDto() {
		
	}
public Long getId() {
	return id;
}
public void setId(Long id) {
	this.id = id;
}
public String getPhoto() {
	return photo;
}
public void setPhoto(String photo) {
	this.photo = photo;
}
public String getLogos() {
	return logos;
}
public void setLogos(String logos) {
	this.logos = logos;
}


public String getStatut_classe() {
	return statut_classe;
}
public void setStatut_classe(String statut_classe) {
	this.statut_classe = statut_classe;
}


public Date getDate_passation() {
	return date_passation;
}
public void setDate_passation(Date date_passation) {
	this.date_passation = date_passation;
}


public String getType_classe() {
	return type_classe;
}
public void setType_classe(String type_classe) {
	this.type_classe = type_classe;
}
@Override
public String toString() {
	return "EleveModelDto [ideleve=" + ideleve + ", noms=" + noms + ", type_classe=" + type_classe + ", nom=" + nom
			+ ", postnom=" + postnom + ", prenom=" + prenom + ", sexe=" + sexe + ", adresse=" + adresse + ", nomtuteur="
			+ nomtuteur + ", email=" + email + ", telephone=" + telephone + ", statut_classe=" + statut_classe
			+ ", matricule=" + matricule + ", pourcentage=" + pourcentage + ", iduser=" + iduser + ", username="
			+ username + ", dateins=" + dateins + ", datenaiss=" + datenaiss + ", date_passation=" + date_passation
			+ ", idintermedaireclasse=" + idintermedaireclasse + ", idintermedaireannee=" + idintermedaireannee
			+ ", idannee=" + idannee + ", annee=" + annee + ", idclasse=" + idclasse + ", classe=" + classe
			+ ", idecole=" + idecole + ", ecole=" + ecole + ", avenue=" + avenue + ", idcommune=" + idcommune
			+ ", commune=" + commune + ", idprovince=" + idprovince + ", province=" + province + ", id=" + id
			+ ", photo=" + photo + ", logos=" + logos + "]";
}
public String getMatricule() {
	return matricule;
}
public void setMatricule(String matricule) {
	this.matricule = matricule;
}
public String getPourcentage() {
	return pourcentage;
}
public void setPourcentage(String pourcentage) {
	this.pourcentage = pourcentage;
}




	
}
