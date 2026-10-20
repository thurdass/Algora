package io.algora.interpreter;
public final class AlgoraException extends RuntimeException {
    private final int line;
    public AlgoraException(int line,String message){super("Linha "+line+": "+message);this.line=line;}
    public int line(){return line;}
}
