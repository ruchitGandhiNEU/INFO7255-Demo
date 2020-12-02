/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.demo.Exceptions;

/**
 *
 * @author Ruchit Gandhi <gandhi.ruc at Northeastern.edu>
 */
public class ObjectNotFoundException extends Exception{
    public ObjectNotFoundException() {
		super();
	}

	public ObjectNotFoundException(String message) {
		super(message);
	}

	public ObjectNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectNotFoundException(Throwable cause) {
		super(cause);
	}
}
