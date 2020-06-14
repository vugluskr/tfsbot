package model;

/**
 * @author Denis Danilin | denis@danilin.name
 * 12.06.2020
 * tfs ☭ sweat and blood
 */
public class Command {
    public CommandType type = CommandType.resetToRoot;
    public int elementIdx;
    public TFile file;
    public String input;
}
