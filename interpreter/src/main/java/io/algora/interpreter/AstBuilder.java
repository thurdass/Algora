package io.algora.interpreter;

import static io.algora.interpreter.Program.*;

import io.algora.interpreter.grammar.*;
import java.util.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

final class AstBuilder extends PortugolBaseVisitor<Object> {
  private String id(String s) {
    return s.toLowerCase(Locale.ROOT);
  }

  private Expression expr(ParseTree c) {
    return (Expression) visit(c);
  }

  private List<Expression> args(PortugolParser.ExprListContext c) {
    return c == null ? List.of() : c.expression().stream().map(this::expr).toList();
  }

  private List<Statement> block(PortugolParser.BlockContext c) {
    return c.statement().stream().map(x -> (Statement) visit(x)).toList();
  }

  private Statement stmt(String kind, ParserRuleContext c, Object... pairs) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2)
      if (pairs[i + 1] != null) m.put((String) pairs[i], pairs[i + 1]);
    return new Statement(kind, c.start.getLine(), Map.copyOf(m));
  }

  private List<Declaration> declarations(PortugolParser.DeclarationsContext c) {
    List<Declaration> result = new ArrayList<>();
    for (ParseTree child : c.children == null ? List.<ParseTree>of() : c.children) {
      if (child instanceof PortugolParser.ConstantDeclContext d)
        result.add(
            new Declaration(
                id(d.ID().getText()),
                "constant",
                List.of(),
                expr(d.expression()),
                d.start.getLine()));
      if (child instanceof PortugolParser.VariableDeclContext d) {
        var type = d.type();
        String scalar = type.scalarType().getText().toLowerCase(Locale.ROOT);
        List<List<Expression>> ranges =
            type.range().stream()
                .map(r -> r.expression().stream().map(this::expr).toList())
                .toList();
        for (var n : d.idList().ID())
          result.add(new Declaration(id(n.getText()), scalar, ranges, null, d.start.getLine()));
      }
    }
    return List.copyOf(result);
  }

  @Override
  public Object visitProgram(PortugolParser.ProgramContext c) {
    List<Routine> routines = new ArrayList<>();
    for (var r : c.routine()) {
      List<Parameter> params = new ArrayList<>();
      if (r.parameters() != null)
        for (var g : r.parameters().parameterGroup())
          for (var n : g.idList().ID())
            params.add(
                new Parameter(
                    id(n.getText()),
                    g.scalarType().getText().toLowerCase(Locale.ROOT),
                    g.VAR() != null));
      routines.add(
          new Routine(
              id(r.ID().getText()),
              List.copyOf(params),
              declarations(r.declarations()),
              block(r.block())));
    }
    return new Program(
        unquote(c.STRING().getText()),
        declarations(c.declarations()),
        List.copyOf(routines),
        block(c.block()));
  }

  private String unquote(String s) {
    return s.substring(1, s.length() - 1).replace("\"\"", "\"");
  }

  @Override
  public Object visitExpression(PortugolParser.ExpressionContext c) {
    return visit(c.logicalOr());
  }

  private Object fold(ParserRuleContext c) {
    Expression left = expr(c.getChild(0));
    for (int i = 1; i < c.getChildCount(); i += 2)
      left =
          new Expression(
              "binary", id(c.getChild(i).getText()), List.of(left, expr(c.getChild(i + 1))));
    return left;
  }

  @Override
  public Object visitLogicalOr(PortugolParser.LogicalOrContext c) {
    return fold(c);
  }

  @Override
  public Object visitLogicalAnd(PortugolParser.LogicalAndContext c) {
    return fold(c);
  }

  @Override
  public Object visitComparison(PortugolParser.ComparisonContext c) {
    return fold(c);
  }

  @Override
  public Object visitAdditive(PortugolParser.AdditiveContext c) {
    return fold(c);
  }

  @Override
  public Object visitMultiplicative(PortugolParser.MultiplicativeContext c) {
    return fold(c);
  }

  @Override
  public Object visitPower(PortugolParser.PowerContext c) {
    return c.power() == null
        ? visit(c.unary())
        : new Expression("binary", "^", List.of(expr(c.unary()), expr(c.power())));
  }

  @Override
  public Object visitUnary(PortugolParser.UnaryContext c) {
    return c.primary() != null
        ? visit(c.primary())
        : new Expression("unary", id(c.getChild(0).getText()), List.of(expr(c.unary())));
  }

  @Override
  public Object visitPrimary(PortugolParser.PrimaryContext c) {
    if (c.NUMBER() != null)
      return new Expression("literal", Double.parseDouble(c.NUMBER().getText()), List.of());
    if (c.STRING() != null)
      return new Expression("literal", unquote(c.STRING().getText()), List.of());
    if (c.VERDADEIRO() != null || c.FALSO() != null)
      return new Expression("literal", c.VERDADEIRO() != null, List.of());
    if (c.ID() != null) return new Expression("call", id(c.ID().getText()), args(c.exprList()));
    if (c.reference() != null) return visit(c.reference());
    return visit(c.expression());
  }

  @Override
  public Object visitReference(PortugolParser.ReferenceContext c) {
    return new Expression("reference", id(c.ID().getText()), args(c.exprList()));
  }

  @Override
  public Object visitAssignmentStmt(PortugolParser.AssignmentStmtContext c) {
    return stmt("assign", c, "target", expr(c.reference()), "value", expr(c.expression()));
  }

  @Override
  public Object visitOutputStmt(PortugolParser.OutputStmtContext c) {
    List<Output> values = new ArrayList<>();
    if (c.outputArgs() != null)
      for (var a : c.outputArgs().outputArg()) {
        var e = a.expression();
        values.add(
            new Output(
                expr(e.get(0)),
                e.size() > 1 ? expr(e.get(1)) : null,
                e.size() > 2 ? expr(e.get(2)) : null));
      }
    return stmt(c.ESCREVAL() != null ? "writeln" : "write", c, "values", List.copyOf(values));
  }

  @Override
  public Object visitInputStmt(PortugolParser.InputStmtContext c) {
    return stmt("read", c, "targets", c.reference().stream().map(this::expr).toList());
  }

  @Override
  public Object visitIfStmt(PortugolParser.IfStmtContext c) {
    return stmt(
        "if",
        c,
        "condition",
        expr(c.expression()),
        "yes",
        block(c.block(0)),
        "no",
        c.block().size() > 1 ? block(c.block(1)) : List.of());
  }

  @Override
  public Object visitWhileStmt(PortugolParser.WhileStmtContext c) {
    return stmt("while", c, "condition", expr(c.expression()), "body", block(c.block()));
  }

  @Override
  public Object visitRepeatStmt(PortugolParser.RepeatStmtContext c) {
    return stmt(
        "repeat",
        c,
        "condition",
        c.expression() == null ? new Expression("literal", false, List.of()) : expr(c.expression()),
        "body",
        block(c.block()));
  }

  @Override
  public Object visitForStmt(PortugolParser.ForStmtContext c) {
    return stmt(
        "for",
        c,
        "name",
        id(c.ID().getText()),
        "from",
        expr(c.expression(0)),
        "to",
        expr(c.expression(1)),
        "step",
        c.expression().size() > 2
            ? expr(c.expression(2))
            : new Expression("literal", 1.0, List.of()),
        "body",
        block(c.block()));
  }

  @Override
  public Object visitChoiceStmt(PortugolParser.ChoiceStmtContext c) {
    List<Case> cases = new ArrayList<>();
    for (var branch : c.caseClause()) {
      List<CaseValue> values =
          branch.caseValue().stream()
              .map(
                  v ->
                      new CaseValue(
                          expr(v.expression(0)),
                          v.expression().size() > 1 ? expr(v.expression(1)) : null))
              .toList();
      cases.add(new Case(values, block(branch.block())));
    }
    return stmt(
        "choice",
        c,
        "value",
        expr(c.expression()),
        "cases",
        List.copyOf(cases),
        "otherwise",
        c.block() == null ? List.of() : block(c.block()));
  }

  @Override
  public Object visitReturnStmt(PortugolParser.ReturnStmtContext c) {
    return stmt("return", c, "value", expr(c.expression()));
  }

  @Override
  public Object visitBreakStmt(PortugolParser.BreakStmtContext c) {
    return stmt("break", c);
  }

  @Override
  public Object visitClearStmt(PortugolParser.ClearStmtContext c) {
    return stmt("clear", c);
  }

  @Override
  public Object visitPauseStmt(PortugolParser.PauseStmtContext c) {
    return stmt("noop", c);
  }

  @Override
  public Object visitDosStmt(PortugolParser.DosStmtContext c) {
    return stmt("noop", c);
  }

  @Override
  public Object visitEchoStmt(PortugolParser.EchoStmtContext c) {
    return stmt("echo", c, "enabled", c.ON() != null);
  }

  @Override
  public Object visitRandomStmt(PortugolParser.RandomStmtContext c) {
    return stmt(
        "random",
        c,
        "enabled",
        c.OFF() == null,
        "args",
        c.expression().stream().map(this::expr).toList());
  }

  @Override
  public Object visitCallStmt(PortugolParser.CallStmtContext c) {
    return stmt(
        "call", c, "value", new Expression("call", id(c.ID().getText()), args(c.exprList())));
  }
}
