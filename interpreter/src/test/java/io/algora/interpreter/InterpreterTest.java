package io.algora.interpreter;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
final class InterpreterTest {
 private String program(String declarations,String body){return "algoritmo \"teste\"\nvar\n"+declarations+"\ninicio\n"+body+"\nfimalgoritmo\n";}
 private Snapshot run(String source){return AlgoraEngine.run(source,null,s->{});}
 private Throwable failure(String source){return assertThrows(CompletionException.class,()->run(source)).getCause();}
 @Test void grammarProducesIndependentAstAndPreservesPortugueseText(){var ast=AlgoraEngine.parse(program("N:inteiro","N <- 2\nSE n > 0 ENTÃO\nescreval(\"ação\")\nfimse"));assertEquals("teste",ast.name());assertEquals("assign",ast.body().getFirst().kind());assertEquals("if",ast.body().getLast().kind());StringBuilder out=new StringBuilder();AlgoraEngine.run(program("","escreval(\"ação\")"),null,out::append);assertEquals("ação\n",out.toString());}
 @Test void antlrReportsInvalidSyntaxAtOriginalLine(){var error=assertThrows(AlgoraException.class,()->AlgoraEngine.parse(program("x:inteiro","se verdadeiro\nx <- 1\nfimse")));assertEquals(5,error.line());assertTrue(error.getMessage().contains("Sintaxe inválida"));assertThrows(AlgoraException.class,()->AlgoraEngine.parse(program("","escreval(\"inacabado)")));}
 @Test void typedInputPrecedenceAndFormattedOutput(){StringBuilder out=new StringBuilder();var result=AlgoraEngine.run(program("n:inteiro\nr:real","leia(n)\nr <- n * 2 + 1\nescreval(\"resultado: \",r:0:2)"),()->"4",out::append);assertEquals("resultado: 9.00\n",out.toString());assertEquals(9.0,result.variables().get("r"));}
 @Test void arraysNegativeBoundsDescendingLoopsAndControl(){var result=run(program("v:vetor[-1..1] de inteiro\ni,s:inteiro","para i de 1 ate -1 passo -1 faca\nv[i] <- i*i\ns <- s+v[i]\nfimpara\nenquanto s < 4 faca\ns <- s + 1\nfimenquanto\nrepita\ns <- s - 1\nate s = 2\nse s=2 entao\ns <- s+10\nsenao\ns <- 0\nfimse"));assertEquals(12.0,result.variables().get("s"));assertEquals(List.of(1.0,0.0,1.0),result.variables().get("v"));}
 @Test void recursionProceduresByReferenceAndLexicalScope(){String source="""
 algoritmo "rotinas"
 var
 x:inteiro
 funcao fat(n:inteiro):inteiro
 inicio
 se n <= 1 entao
 retorne 1
 fimse
 retorne n * fat(n-1)
 fimfuncao
 procedimento dobro(var n:inteiro)
 inicio
 n <- n * 2
 fimprocedimento
 funcao global():inteiro
 inicio
 retorne x
 fimfuncao
 funcao caller():inteiro
 var
 x:inteiro
 inicio
 x <- 99
 retorne global()
 fimfuncao
 inicio
 x <- fat(5)
 dobro(x)
 x <- caller()
 fimalgoritmo
 """;assertEquals(240.0,run(source).variables().get("x"));}
 @Test void caseRangesShortCircuitAndUnconditionalRepeat(){var result=run(program("n:inteiro","n <- 5\nescolha n\ncaso 4,5 ate 7\nn <- 8\noutrocaso\nn <- 0\nfimescolha\nse falso e 1/0 = 1 entao\nn <- 0\nfimse\nrepita\nn <- n + 1\nse n=10 entao\ninterrompa\nfimse\nfimrepita"));assertEquals(10.0,result.variables().get("n"));}
 @Test void constantsBoundArraysAndRejectWrites(){String source="algoritmo \"const\"\nconst\nbase=2\nmax=base*3+1\nvar\nv:vetor[-base..max] de inteiro\ninicio\nv[max] <- base\nfimalgoritmo";assertEquals(7.0,run(source).variables().get("max"));assertTrue(failure(source.replace("v[max] <- base","base <- 9")).getMessage().contains("constante"));}
 @Test void diagnosticsRejectBoundsZeroDivisionAndInfiniteLoop(){assertTrue(failure(program("v:vetor[1..2] de inteiro","v[0] <- 1")).getMessage().contains("Índice fora"));assertTrue(failure(program("x:real","x <- 1/0")).getMessage().contains("Divisão por zero"));var session=AlgoraEngine.createSession(program("","enquanto verdadeiro faca\nfimenquanto"),null,null,null).limits(10,10);assertTrue(assertThrows(CompletionException.class,()->session.start(false).join()).getCause().getMessage().contains("Limite de instruções"));assertThrows(AlgoraException.class,()->AlgoraEngine.createSession(program("v:vetor[1..10000,1..10000] de inteiro",""),null,null,null));}
 @Test void debuggerBreakpointsInspectBeforeExecutionAndResume()throws Exception{var paused=new LinkedBlockingQueue<Snapshot>();var session=AlgoraEngine.createSession(program("x:inteiro","x <- 1\nx <- 2\nx <- 3"),null,null,s->{if(s.status()==Status.PAUSED)paused.add(s);});session.setBreakpoints(Set.of(6));var future=session.start(true);var first=paused.poll(5,TimeUnit.SECONDS);assertNotNull(first);assertEquals(5,first.line());assertEquals(0.0,first.variables().get("x"));session.step();var second=paused.poll(5,TimeUnit.SECONDS);assertNotNull(second);assertEquals(6,second.line());assertEquals(1.0,second.variables().get("x"));session.resume();assertEquals(3.0,future.get(5,TimeUnit.SECONDS).variables().get("x"));}
 @Test void stopCancelsPausedAndBlockedInput()throws Exception{var pauses=new CountDownLatch(1);var paused=AlgoraEngine.createSession(program("x:inteiro","x <- 1"),null,null,s->{if(s.status()==Status.PAUSED)pauses.countDown();});var a=paused.start(true);assertTrue(pauses.await(5,TimeUnit.SECONDS));paused.stop();assertEquals(Status.STOPPED,a.get(5,TimeUnit.SECONDS).status());var reads=new CountDownLatch(1);var blocked=AlgoraEngine.createSession(program("x:inteiro","leia(x)"),()->{reads.countDown();Thread.sleep(60000);return "0";},null,null);var b=blocked.start(false);assertTrue(reads.await(5,TimeUnit.SECONDS));blocked.stop();assertEquals(Status.STOPPED,b.get(5,TimeUnit.SECONDS).status());}
 @Test void documentedMathSignaturesAndBarePiRand(){var result=AlgoraEngine.createSession(program("a,b,c,d:real","a <- exp(2,9)\nb <- radpgrau(pi)\nc <- arcsen(1)+arccos(0)\nd <- rand"),null,null,null).random(()->0.25).start(false).join();assertEquals(512.0,result.variables().get("a"));assertEquals(180.0,result.variables().get("b"));assertEquals(Math.PI,(Double)result.variables().get("c"),1e-12);assertEquals(0.25,result.variables().get("d"));assertTrue(failure(program("x:real","x <- exp(2)")).getMessage().contains("argumentos"));}
 @Test void textFunctionsMatchDocumentedOrdering(){StringBuilder out=new StringBuilder();AlgoraEngine.run(program("","escreval(pos(\"Brasil\",\"O melhor do Brasil\"),\" \",copia(\"abcdef\",2,3),\" \",asc(\"B\"),\" \",carac(65))"),null,out::append);assertEquals("13 bcd 66 A\n",out.toString());}
 @Test void randomInputPrecisionEchoAndRestoredHumanInput(){StringBuilder out=new StringBuilder();AtomicInteger inputs=new AtomicInteger();var result=AlgoraEngine.createSession(program("x:inteiro\ny:real","eco off\naleatorio 1,2,2\nleia(y)\neco on\naleatorio 7,7\nleia(x)\naleatorio off\nleia(x)"),()->{inputs.incrementAndGet();return "9";},out::append,null).random(()->0.5).start(false).join();assertEquals(1.5,result.variables().get("y"));assertEquals(9.0,result.variables().get("x"));assertEquals(1,inputs.get());assertEquals("7\n",out.toString());}
}
