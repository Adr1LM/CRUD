package com.paellasoft.CRUD.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name="boe")
public class Boe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contenido_original",columnDefinition = "LONGTEXT")
    private String contenidoOriginal;
    @Column(name = "contenido_resumido",columnDefinition = "LONGTEXT")
    private String contenidoResumido;
    @Column(name="fecha_boe")
    private String fechaBoe;

    @OneToMany(mappedBy = "boe")
    private List<BoeUser> subscriptions;

    public void setFechaBoe(String fechaBoe) {
        this.fechaBoe = fechaBoe;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContenidoOriginal() {
        return contenidoOriginal;
    }

    public void setContenidoOriginal(String contenidoOriginal) {
        this.contenidoOriginal = contenidoOriginal;
    }

    public String getContenidoResumido() {
        return contenidoResumido;
    }

    public void setContenidoResumido(String contenidoResumido) {
        this.contenidoResumido = contenidoResumido;
    }

    public String getFechaBoe() {
        return fechaBoe;
    }


    @Override
    public String toString() {
        return "Boe " + fechaBoe.substring(0,10) +"\n"+ contenidoResumido + "\n\n";
    }



    public List<BoeUser> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<BoeUser> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
