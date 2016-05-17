package com.artifex.mupdfdemo;

public class Separation
{
	public String name;
	public int rgba;
	int cmyk;

	public Separation(String name, int rgba, int cmyk)
	{
		this.name = name;
		this.rgba = rgba;
		this.cmyk = cmyk;
	}
}
