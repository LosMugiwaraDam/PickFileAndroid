package com.example.pruebarutas;

import java.io.Serializable;

public class Archivo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String nombre;
	public String ext;
	public String type;
	public byte[] data;

	public Archivo(String nombreC, String type, byte[] data) {
		super();
		String[] aux = nombreC.split("\\.");
		this.nombre = aux[0];
		this.ext = aux[aux.length - 1];
		this.type = type;
		this.data = data;
	}

	public String getNombre() {
		return nombre + "." + ext;
	}
}
