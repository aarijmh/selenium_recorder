package io.auton8.recorder.model;

import java.util.List;

import lombok.Data;

@Data
public class Test {
private String id;
private String name;
private List<Command> commands;
}
