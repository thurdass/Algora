package io.algora.interpreter;
import java.util.*;
/** Immutable AST independent from ANTLR and the desktop UI. */
public record Program(String name,List<Declaration> declarations,List<Routine> routines,List<Statement> body) {
 public record Expression(String kind,Object value,List<Expression> children){}
 public record Declaration(String name,String type,List<List<Expression>> ranges,Expression constant){}
 public record Parameter(String name,String type,boolean reference){}
 public record Routine(String name,List<Parameter> parameters,List<Declaration> declarations,List<Statement> body){}
 public record Statement(String kind,int line,Map<String,Object> fields) {
  @SuppressWarnings("unchecked") public <T>T field(String name){return (T)fields.get(name);}
 }
 public record Output(Expression value,Expression width,Expression precision){}
 public record CaseValue(Expression from,Expression to){}
 public record Case(List<CaseValue> values,List<Statement> body){}
}
