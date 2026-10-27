package com.clinica.entidades;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "seguros")
public class Seguro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "paciente_id", unique = true, nullable = false)
    private Paciente paciente;

    @Column(name = "numero_afiliacion", unique = true, nullable = false, length = 50)
    private String numeroAfiliacion;

    @Enumerated(EnumType.STRING)
    private EstadoSeguro estado = EstadoSeguro.ACTIVO;

    @ManyToOne
    @JoinColumn(name = "establecimiento_id")
    private Establecimiento establecimiento;

    @Column(name = "cuota_mensual")
    private Double cuotaMensual = 50.0;

    @Column(name = "fecha_afiliacion")
    private LocalDate fechaAfiliacion;

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public String getNumeroAfiliacion() { return numeroAfiliacion; }
    public void setNumeroAfiliacion(String numeroAfiliacion) { this.numeroAfiliacion = numeroAfiliacion; }
    public EstadoSeguro getEstado() { return estado; }
    public void setEstado(EstadoSeguro estado) { this.estado = estado; }
    public Establecimiento getEstablecimiento() { return establecimiento; }
    public void setEstablecimiento(Establecimiento establecimiento) { this.establecimiento = establecimiento; }
    public Double getCuotaMensual() { return cuotaMensual; }
    public void setCuotaMensual(Double cuotaMensual) { this.cuotaMensual = cuotaMensual; }
    public LocalDate getFechaAfiliacion() { return fechaAfiliacion; }
    public void setFechaAfiliacion(LocalDate fechaAfiliacion) { this.fechaAfiliacion = fechaAfiliacion; }
}