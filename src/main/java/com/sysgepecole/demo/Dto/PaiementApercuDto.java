package com.sysgepecole.demo.Dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PaiementApercuDto {

    private Long ideleve;
    private String nomEleve;
    private List<BlocPaiementDto> blocs = new ArrayList<>();

    public PaiementApercuDto() {}

    public PaiementApercuDto(Long ideleve, String nomEleve, List<BlocPaiementDto> blocs) {
        this.ideleve = ideleve;
        this.nomEleve = nomEleve;
        this.blocs = blocs;
    }

    public Long getIdeleve() { return ideleve; }
    public void setIdeleve(Long ideleve) { this.ideleve = ideleve; }

    public String getNomEleve() { return nomEleve; }
    public void setNomEleve(String nomEleve) { this.nomEleve = nomEleve; }

    public List<BlocPaiementDto> getBlocs() { return blocs; }
    public void setBlocs(List<BlocPaiementDto> blocs) { this.blocs = blocs; }

    // ===== BlocPaiementDto (classe interne statique, visible partout) =====
    public static class BlocPaiementDto {
        private Long idintermedaireclasse;
        private String classeLabel;
        private Double montantPaye = 0.0;
        private Double montantFrais = 0.0;
        private Double dette = 0.0;
        private String statut; // ex: "CLASSE ACTUELLE","CLASSE PRECEDENTE","DETTE","SOLDER"
        private List<PaiementDetailDto> details = new ArrayList<>();

        public BlocPaiementDto() {}

        public BlocPaiementDto(Long idintermedaireclasse, String classeLabel,
                               Double montantPaye, Double montantFrais, Double dette, String statut) {
            this.idintermedaireclasse = idintermedaireclasse;
            this.classeLabel = classeLabel;
            this.montantPaye = montantPaye;
            this.montantFrais = montantFrais;
            this.dette = dette;
            this.statut = statut;
        }

        public Long getIdintermedaireclasse() { return idintermedaireclasse; }
        public void setIdintermedaireclasse(Long idintermedaireclasse) { this.idintermedaireclasse = idintermedaireclasse; }

        public String getClasseLabel() { return classeLabel; }
        public void setClasseLabel(String classeLabel) { this.classeLabel = classeLabel; }

        public Double getMontantPaye() { return montantPaye; }
        public void setMontantPaye(Double montantPaye) { this.montantPaye = montantPaye; }

        public Double getMontantFrais() { return montantFrais; }
        public void setMontantFrais(Double montantFrais) { this.montantFrais = montantFrais; }

        public Double getDette() { return dette; }
        public void setDette(Double dette) { this.dette = dette; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public List<PaiementDetailDto> getDetails() { return details; }
        public void setDetails(List<PaiementDetailDto> details) { this.details = details; }
    }

    // Classe de détail de paiement (catégorie/tranche si besoin)
    public static class PaiementDetailDto {
        private String tranche;
        private String categorie;
        private Double montantPaye;
        private String statut;

        public PaiementDetailDto() {}

        public PaiementDetailDto(String tranche, String categorie, Double montantPaye, String statut) {
            this.tranche = tranche;
            this.categorie = categorie;
            this.montantPaye = montantPaye;
            this.statut = statut;
        }

        public String getTranche() { return tranche; }
        public void setTranche(String tranche) { this.tranche = tranche; }

        public String getCategorie() { return categorie; }
        public void setCategorie(String categorie) { this.categorie = categorie; }

        public Double getMontantPaye() { return montantPaye; }
        public void setMontantPaye(Double montantPaye) { this.montantPaye = montantPaye; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaiementApercuDto)) return false;
        PaiementApercuDto that = (PaiementApercuDto) o;
        return Objects.equals(ideleve, that.ideleve);
    }

    @Override
    public int hashCode() { return Objects.hash(ideleve); }
}
