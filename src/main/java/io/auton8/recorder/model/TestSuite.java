package io.auton8.recorder.model;

import java.util.HashMap;
import java.util.List;

import lombok.Data;

@Data
public class TestSuite {

	private String id;
	private String version;
	private String name;
	private String url;
	private List<Test> tests;

	public void updateTestMap() {
		for (Test test : tests) {
			if (test.getCommands() != null) {
				for (Command command : test.getCommands()) {
					command.setTargetMap( new HashMap<>());
					for(List<String> targets : command.getTargets()) {
						if(targets.size() >= 2) {
							command.getTargetMap().put(targets.get(1), targets.get(0));
						}
					}
				}
			}
		}
	}
}
