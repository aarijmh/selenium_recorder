package io.auton8.recorder.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Command {
	private String id;
	private String comment;
	private String command;
	private String target;
	private List<List<String>> targets;
	private String value;
	private Map<String, String> targetMap;
	
	public void setTargets(List<List<String>> targets) {
		this.targets = targets;
		System.out.println("ASDASDSAD");
	}
}
