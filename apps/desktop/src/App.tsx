import { useCallback, useEffect, useRef, useState } from "react";
import Editor, { loader, type OnMount } from "@monaco-editor/react";
import * as monaco from "monaco-editor";
import EditorWorker from "monaco-editor/editor/editor.worker.js?worker";
import { createInterpreter } from "@algora/core";
import {
  Braces,
  Play,
  Bug,
  Square,
  StepForward,
  Pause,
  FolderOpen,
  FileCode2,
  Plus,
  X,
  ChevronRight,
  ChevronDown,
  Terminal,
  Settings2,
  Save,
  BookOpen,
  ArrowUp,
  Check,
} from "lucide-react";

self.MonacoEnvironment = { getWorker: () => new EditorWorker() };
loader.config({ monaco });
type FileTab = {
  id: number;
  name: string;
  content: string;
  saved: string;
  path?: string;
};
type Snapshot = { line?: number; variables?: unknown; status?: string };
type Interpreter = ReturnType<typeof createInterpreter>;
declare global {
  interface Window {
    algora?: {
      openFile(): Promise<{
        path: string;
        name?: string;
        content: string;
      } | null>;
      saveFile(file: {
        path?: string;
        content: string;
      }): Promise<{ path: string } | null>;
    };
  }
}
const initial = `algoritmo "Boas-vindas"\n// Todo grande projeto começa com uma ideia.\nvar\n   nome: caractere\n   contador: inteiro\ninicio\n   escreval("Olá! Vamos criar algo incrível?")\n   escreva("Qual é o seu nome? ")\n   leia(nome)\n   escreval("Bem-vindo ao Algora, ", nome, "!")\n\n   para contador de 1 ate 3 faca\n      escreval("Sua próxima ideia começa aqui. Passo ", contador)\n   fimpara\nfimalgoritmo\n`;
const keywords =
  "algoritmo var inicio fimalgoritmo inteiro real caractere caracter logico verdadeiro falso se entao senao fimse para de ate passo faca fimpara enquanto fimenquanto repita ate escolha caso outrocaso fimescolha procedimento fimprocedimento funcao fimfuncao retorne leia escreva escreval e ou nao div mod interrompa".split(
    " ",
  );
let registered = false;
function setupLanguage() {
  if (registered) return;
  registered = true;
  monaco.languages.register({ id: "visualg" });
  monaco.languages.setMonarchTokensProvider("visualg", {
    ignoreCase: true,
    keywords,
    tokenizer: {
      root: [
        [/\/\/.*$/, "comment"],
        [/"[^"\n]*"|'[^'\n]*'/, "string"],
        [/\b\d+(\.\d+)?\b/, "number"],
        [
          /[a-zA-Z_À-ÿ][\wÀ-ÿ]*/,
          { cases: { "@keywords": "keyword", "@default": "identifier" } },
        ],
        [/<-|[+*/=<>-]/, "operator"],
      ],
    },
  });
  monaco.languages.setLanguageConfiguration("visualg", {
    comments: { lineComment: "//" },
    brackets: [
      ["(", ")"],
      ["[", "]"],
    ],
    autoClosingPairs: [
      { open: '"', close: '"' },
      { open: "(", close: ")" },
      { open: "[", close: "]" },
    ],
  });
  monaco.languages.registerCompletionItemProvider("visualg", {
    provideCompletionItems(model, position) {
      const w = model.getWordUntilPosition(position);
      const range = new monaco.Range(
        position.lineNumber,
        w.startColumn,
        position.lineNumber,
        w.endColumn,
      );
      return {
        suggestions: [
          ...keywords.map((label) => ({
            label,
            kind: monaco.languages.CompletionItemKind.Keyword,
            insertText: label,
            range,
          })),
          ...[
            {
              label: "se…entao",
              insertText: "se ${1:condicao} entao\n   $0\nfimse",
            },
            {
              label: "para…faca",
              insertText:
                "para ${1:i} de ${2:1} ate ${3:10} faca\n   $0\nfimpara",
            },
            {
              label: "enquanto…faca",
              insertText: "enquanto ${1:condicao} faca\n   $0\nfimenquanto",
            },
          ].map((s) => ({
            ...s,
            range,
            kind: monaco.languages.CompletionItemKind.Snippet,
            insertTextRules:
              monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          })),
        ],
      };
    },
  });
  monaco.editor.defineTheme("algora", {
    base: "vs-dark",
    inherit: true,
    rules: [
      { token: "keyword", foreground: "C29BFF" },
      { token: "string", foreground: "A8D6AB" },
      { token: "comment", foreground: "65677E", fontStyle: "italic" },
      { token: "number", foreground: "F0BB87" },
      { token: "operator", foreground: "75D8DF" },
    ],
    colors: {
      "editor.background": "#13131D",
      "editor.foreground": "#C8C9D9",
      "editorLineNumber.foreground": "#4C4C63",
      "editorLineNumber.activeForeground": "#D0C0FA",
      "editor.lineHighlightBackground": "#1B1B2A",
      "editor.selectionBackground": "#47346977",
      "editorCursor.foreground": "#BA9AF9",
      "editorIndentGuide.background1": "#252535",
      "editorWidget.background": "#20202E",
      "editorGutter.background": "#13131D",
    },
  });
}
function displayVariables(value: unknown): [string, string][] {
  if (!value) return [];
  const entries =
    value instanceof Map
      ? [...value.entries()]
      : Array.isArray(value)
        ? value.map((v, i) => [v.name ?? i, v.value ?? v])
        : Object.entries(value);
  return entries.map(([k, v]) => [
    String(k),
    typeof v === "object" && v !== null && "value" in v
      ? JSON.stringify(v.value)
      : JSON.stringify(v),
  ]);
}
export default function App() {
  const [files, setFiles] = useState<FileTab[]>([
    { id: 1, name: "boas-vindas.alg", content: initial, saved: initial },
  ]);
  const [active, setActive] = useState(1);
  const file = files.find((f) => f.id === active)!;
  const [output, setOutput] = useState<{ text: string; kind?: string }[]>([]);
  const [snapshot, setSnapshot] = useState<Snapshot>({ status: "idle" });
  const [breakpoints, setBreakpoints] = useState<number[]>([]);
  const [input, setInput] = useState("");
  const [waiting, setWaiting] = useState(false);
  const [cursor, setCursor] = useState({ lineNumber: 1, column: 1 });
  const [sidebar, setSidebar] = useState(true);
  const [bottom, setBottom] = useState<"terminal" | "help">("terminal");
  const interpreter = useRef<Interpreter | null>(null);
  const inputResolver = useRef<((v: string) => void) | null>(null);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const decorations = useRef<monaco.editor.IEditorDecorationsCollection | null>(
    null,
  );
  const terminalRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const busy = ["running", "paused", "waiting", "waiting-input"].includes(
    snapshot.status ?? "",
  );
  const append = (text: string, kind?: string) =>
    setOutput((o) =>
      text.includes("\u001b[2J")
        ? [{ text: text.replace(/\u001b\[[0-9;]*[A-Za-z]/g, ""), kind }]
        : [...o, { text, kind }],
    );
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (files.some((f) => f.content !== f.saved)) {
        e.preventDefault();
        e.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [files]);
  const stop = useCallback(() => {
    interpreter.current?.stop();
    inputResolver.current?.("");
    inputResolver.current = null;
    setWaiting(false);
    setSnapshot((s) => ({ ...s, status: "stopped", line: undefined }));
  }, []);
  const run = async (debug = false) => {
    if (busy) {
      if (snapshot.status === "paused") await interpreter.current?.continue();
      return;
    }
    setOutput([]);
    setBottom("terminal");
    setSnapshot({ status: "running" });
    try {
      const current = createInterpreter(file.content, {
        output: (text: string) => append(text),
        input: async () => {
          setWaiting(true);
          setTimeout(() => inputRef.current?.focus(), 20);
          return new Promise<string>((resolve) => {
            inputResolver.current = resolve;
          });
        },
        onStep: (s: Snapshot) => setSnapshot({ ...s }),
        maxSteps: 100000,
      });
      interpreter.current = current;
      current.setBreakpoints(breakpoints);
      if (debug) current.pause();
      await current.run();
      setSnapshot({ ...current.getSnapshot() });
    } catch (error) {
      if (error instanceof Error && error.name === "AbortError") {
        setSnapshot((s) => ({ ...s, status: "stopped" }));
      } else {
        append(error instanceof Error ? error.message : String(error), "error");
        setSnapshot((s) => ({ ...s, status: "error" }));
      }
    } finally {
      setWaiting(false);
    }
  };
  const openFile = async () => {
    if (busy) return;
    if (window.algora) {
      const f = await window.algora.openFile();
      if (f) {
        const id = Date.now();
        setFiles((fs) => [
          ...fs,
          {
            ...f,
            id,
            name: f.name ?? f.path.split(/[\\/]/).pop()!,
            saved: f.content,
          },
        ]);
        setActive(id);
      }
    } else {
      const picker = document.createElement("input");
      picker.type = "file";
      picker.accept = ".alg";
      picker.onchange = async () => {
        const f = picker.files?.[0];
        if (f) {
          const bytes = await f.arrayBuffer();
          let content = new TextDecoder("utf-8").decode(bytes);
          if (content.includes("�"))
            content = new TextDecoder("windows-1252").decode(bytes);
          const id = Date.now();
          setFiles((fs) => [
            ...fs,
            { id, name: f.name, content, saved: content },
          ]);
          setActive(id);
        }
      };
      picker.click();
    }
  };
  const saveFile = async () => {
    if (window.algora) {
      const result = await window.algora.saveFile({
        path: file.path,
        content: file.content,
      });
      if (result)
        setFiles((fs) =>
          fs.map((f) =>
            f.id === active
              ? {
                  ...f,
                  path: result.path,
                  name: result.path.split(/[\\/]/).pop()!,
                  saved: f.content,
                }
              : f,
          ),
        );
    } else {
      const a = document.createElement("a");
      a.href = URL.createObjectURL(
        new Blob([file.content], { type: "text/plain;charset=utf-8" }),
      );
      a.download = file.name;
      a.click();
      URL.revokeObjectURL(a.href);
      setFiles((fs) =>
        fs.map((f) => (f.id === active ? { ...f, saved: f.content } : f)),
      );
    }
  };
  const newFile = () => {
    if (busy) return;
    const id = Date.now();
    setFiles((fs) => [
      ...fs,
      {
        id,
        name: `algoritmo-${fs.length + 1}.alg`,
        content: 'algoritmo "Novo algoritmo"\nvar\ninicio\n   \nfimalgoritmo\n',
        saved: "",
      },
    ]);
    setActive(id);
  };
  const toggleBreakpoint = (line: number) =>
    setBreakpoints((b) =>
      b.includes(line) ? b.filter((n) => n !== line) : [...b, line],
    );
  useEffect(() => {
    interpreter.current?.setBreakpoints(breakpoints);
    decorations.current?.set([
      ...breakpoints.map((line) => ({
        range: new monaco.Range(line, 1, line, 1),
        options: {
          glyphMarginClassName: "breakpoint-dot",
          glyphMarginHoverMessage: { value: "Ponto de parada" },
        },
      })),
      ...(snapshot.line && busy
        ? [
            {
              range: new monaco.Range(snapshot.line, 1, snapshot.line, 1),
              options: {
                isWholeLine: true,
                className: "execution-line",
                glyphMarginClassName: "execution-arrow",
              },
            },
          ]
        : []),
    ]);
    if (snapshot.line && busy)
      editorRef.current?.revealLineInCenterIfOutsideViewport(snapshot.line);
  }, [breakpoints, snapshot, busy]);
  useEffect(() => {
    terminalRef.current?.scrollTo(0, terminalRef.current.scrollHeight);
  }, [output, waiting]);
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "s") {
        e.preventDefault();
        void saveFile();
      }
      if ((e.ctrlKey || e.metaKey) && e.key === "o") {
        e.preventDefault();
        void openFile();
      }
      if (e.key === "F5") {
        e.preventDefault();
        void run();
      }
      if (e.key === "F10") {
        e.preventDefault();
        void interpreter.current?.step();
      }
      if (e.key === "F9") {
        e.preventDefault();
        toggleBreakpoint(editorRef.current?.getPosition()?.lineNumber ?? 1);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  });
  const mount: OnMount = (editor) => {
    editorRef.current = editor;
    decorations.current = editor.createDecorationsCollection();
    editor.onDidChangeCursorPosition((e) => setCursor(e.position));
    editor.onMouseDown((e) => {
      if (
        e.target.type === monaco.editor.MouseTargetType.GUTTER_GLYPH_MARGIN &&
        e.target.position
      )
        toggleBreakpoint(e.target.position.lineNumber);
    });
  };
  const vars = displayVariables(snapshot.variables);
  return (
    <div className="app">
      <header className="titlebar">
        <div className="brand">
          <span className="brand-mark">
            <Braces size={21} />
          </span>
          algora<span className="brand-tag">IDE</span>
        </div>
        <nav>
          <button onClick={newFile}>Arquivo</button>
          <button onClick={() => editorRef.current?.focus()}>Editar</button>
          <button onClick={() => void run()}>Executar</button>
          <button onClick={() => setBottom("help")}>Ajuda</button>
        </nav>
        <div className="title-context">
          <span className="small-dot" /> Seu próximo algoritmo começa aqui
        </div>
        <button
          className="icon-button"
          title="Salvar arquivo (Ctrl+S)"
          onClick={() => void saveFile()}
        >
          <Save size={16} />
        </button>
      </header>
      <div className="workspace">
        <aside className="activity">
          <button
            title="Explorador"
            className={sidebar ? "selected" : ""}
            onClick={() => setSidebar(!sidebar)}
          >
            <FolderOpen size={23} />
          </button>
          <button
            title="Depurador"
            onClick={() => {
              setSidebar(true);
            }}
          >
            <Bug size={23} />
          </button>
          <button title="Guia rápido" onClick={() => setBottom("help")}>
            <BookOpen size={22} />
          </button>
          <div className="activity-bottom">
            <span className="avatar">A</span>
          </div>
        </aside>
        {sidebar && (
          <aside className="sidebar">
            <div className="panel-heading">
              EXPLORADOR
              <div>
                <button title="Novo algoritmo" onClick={newFile}>
                  <Plus size={15} />
                </button>
                <button title="Abrir arquivo" onClick={() => void openFile()}>
                  <FolderOpen size={15} />
                </button>
              </div>
            </div>
            <div className="project-heading">
              <ChevronDown size={14} />
              <span>MEUS ALGORITMOS</span>
            </div>
            <div className="file-list">
              {files.map((f) => (
                <button
                  key={f.id}
                  className={active === f.id ? "file active" : "file"}
                  onClick={() => {
                    if (busy) return;
                    setActive(f.id);
                    setBreakpoints([]);
                  }}
                >
                  <FileCode2 size={16} />
                  <span>{f.name}</span>
                  {f.content !== f.saved && <i />}
                </button>
              ))}
            </div>
            <button className="new-file" onClick={newFile}>
              <Plus size={15} />
              Novo algoritmo
            </button>
            <div className="sidebar-footer">
              <div className="hint-icon">
                <Braces size={19} />
              </div>
              <strong>Ideias viram algoritmos.</strong>
              <p>
                Explore, experimente e aprenda
                <br />
                uma linha de cada vez.
              </p>
              <button onClick={() => setBottom("help")}>
                Ver guia rápido <ChevronRight size={13} />
              </button>
            </div>
          </aside>
        )}
        <main>
          <div className="editor-tabs">
            {files.map((f) => (
              <div
                key={f.id}
                className={`tab ${active === f.id ? "active" : ""}`}
              >
                <button
                  onClick={() => {
                    if (busy) return;
                    setActive(f.id);
                    setBreakpoints([]);
                  }}
                >
                  <FileCode2 size={15} />
                  {f.name}
                  {f.content !== f.saved && <i />}
                </button>
                <button
                  title="Fechar arquivo"
                  onClick={() => {
                    if (busy) return;
                    if (
                      f.content !== f.saved &&
                      !window.confirm(
                        "Este arquivo tem alterações não salvas. Fechar mesmo assim?",
                      )
                    )
                      return;
                    if (files.length === 1) return;
                    const next = files.filter((x) => x.id !== f.id);
                    setFiles(next);
                    if (active === f.id) setActive(next[0].id);
                  }}
                >
                  <X size={13} />
                </button>
              </div>
            ))}
            <button
              className="icon-button"
              title="Novo arquivo"
              onClick={newFile}
            >
              <Plus size={16} />
            </button>
          </div>
          <div className="editor-toolbar">
            <div className="breadcrumb">
              <span>algoritmos</span>
              <ChevronRight size={12} />
              <FileCode2 size={13} />
              <span>{file.name}</span>
            </div>
            <div className="run-controls">
              <button
                className="run-button"
                onClick={() => void run()}
                title="Executar (F5)"
              >
                <Play size={13} fill="currentColor" />
                {snapshot.status === "paused" ? "Continuar" : "Executar"}
                <kbd>F5</kbd>
              </button>
              <span className="divider" />
              <button
                title="Depurar: pontos de parada (F9)"
                onClick={() => void run(true)}
              >
                <Bug size={16} />
              </button>
              <button
                title="Pausar"
                disabled={!busy}
                onClick={() => interpreter.current?.pause()}
              >
                <Pause size={15} />
              </button>
              <button
                title="Próximo passo (F10)"
                disabled={!busy}
                onClick={() => void interpreter.current?.step()}
              >
                <StepForward size={17} />
              </button>
              <button title="Parar execução" disabled={!busy} onClick={stop}>
                <Square size={13} />
              </button>
            </div>
          </div>
          <section className="editor-area">
            <Editor
              path={String(file.id)}
              language="visualg"
              theme="algora"
              value={file.content}
              beforeMount={setupLanguage}
              onMount={mount}
              onChange={(value) =>
                setFiles((fs) =>
                  fs.map((f) =>
                    f.id === active ? { ...f, content: value ?? "" } : f,
                  ),
                )
              }
              options={{
                fontFamily:
                  '"JetBrains Mono", "Cascadia Code", "SFMono-Regular", Consolas, monospace',
                readOnly: busy,
                fontSize: 14,
                lineHeight: 24,
                minimap: { enabled: false },
                padding: { top: 18 },
                glyphMargin: true,
                scrollBeyondLastLine: false,
                smoothScrolling: true,
                renderLineHighlight: "all",
                overviewRulerLanes: 0,
                hideCursorInOverviewRuler: true,
                automaticLayout: true,
                tabSize: 3,
                wordWrap: "on",
                quickSuggestions: true,
              }}
            />
          </section>
          <section className="bottom-panel">
            <div className="bottom-tabs">
              <button
                className={bottom === "terminal" ? "active" : ""}
                onClick={() => setBottom("terminal")}
              >
                <Terminal size={14} />
                TERMINAL<span className="terminal-count">1</span>
              </button>
              <button
                className={bottom === "help" ? "active" : ""}
                onClick={() => setBottom("help")}
              >
                <BookOpen size={13} />
                GUIA RÁPIDO
              </button>
              <div className="terminal-tools">
                <span className={`status-dot ${busy ? "live" : ""}`} />
                {waiting
                  ? "Aguardando entrada"
                  : snapshot.status === "paused"
                    ? "Pausado"
                    : busy
                      ? "Em execução"
                      : "Pronto"}
                <button title="Limpar terminal" onClick={() => setOutput([])}>
                  <X size={14} />
                </button>
              </div>
            </div>
            {bottom === "terminal" ? (
              <div className="terminal-content" ref={terminalRef}>
                {output.length === 0 ? (
                  <>
                    <div className="terminal-welcome">
                      <span>Algora</span> · seu espaço para experimentar
                    </div>
                    <div className="terminal-muted">
                      Execute seu algoritmo para ver a saída aqui. <kbd>F5</kbd>
                    </div>
                  </>
                ) : (
                  output.map((o, i) => (
                    <span
                      className={
                        o.kind === "error" ? "terminal-error" : "output"
                      }
                      key={i}
                    >
                      {o.text}
                    </span>
                  ))
                )}
                {waiting && (
                  <form
                    className="terminal-input"
                    onSubmit={(e) => {
                      e.preventDefault();
                      append(input + "\n", "input");
                      inputResolver.current?.(input);
                      inputResolver.current = null;
                      setInput("");
                      setWaiting(false);
                    }}
                  >
                    <ChevronRight size={15} />
                    <input
                      ref={inputRef}
                      aria-label="Entrada do algoritmo"
                      value={input}
                      onChange={(e) => setInput(e.target.value)}
                      autoFocus
                      placeholder="Digite um valor e pressione Enter"
                    />
                    <button title="Enviar valor">
                      <ArrowUp size={14} />
                    </button>
                  </form>
                )}
                {!waiting && output.length > 0 && !busy && (
                  <div className="terminal-finished">
                    {snapshot.status === "error"
                      ? "Execução interrompida com erro."
                      : "Execução finalizada."}
                  </div>
                )}
              </div>
            ) : (
              <div className="quick-help">
                <div>
                  <strong>Do primeiro passo à próxima ideia.</strong>
                  <p>
                    Escreva seu algoritmo em Portugol e pressione F5. Clique na
                    margem esquerda para adicionar pontos de parada.
                  </p>
                </div>
                <span>
                  <kbd>Ctrl O</kbd> Abrir .alg
                </span>
                <span>
                  <kbd>Ctrl S</kbd> Salvar
                </span>
                <span>
                  <kbd>F9</kbd> Ponto de parada
                </span>
                <span>
                  <kbd>F10</kbd> Próximo passo
                </span>
              </div>
            )}
          </section>
        </main>
        <aside className="debug-panel">
          <div className="panel-heading">
            <span>INSPEÇÃO</span>
            <Settings2 size={14} />
          </div>
          <div className="debug-heading">
            <ChevronDown size={13} />
            VARIÁVEIS<span>{vars.length}</span>
          </div>
          {vars.length ? (
            <div className="variables">
              {vars.map(([name, value]) => (
                <div className="variable" key={name}>
                  <span>{name}</span>
                  <code title={value}>{value}</code>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-vars">
              <span>
                <Braces size={25} />
              </span>
              <p>Um olhar por dentro.</p>
              <small>
                Execute seu algoritmo para
                <br />
                acompanhar as variáveis.
              </small>
            </div>
          )}
          <div className="debug-heading breakpoints-heading">
            <ChevronDown size={13} />
            PONTOS DE PARADA<span>{breakpoints.length}</span>
          </div>
          {breakpoints.length ? (
            [...breakpoints]
              .sort((a, b) => a - b)
              .map((line) => (
                <button
                  className="breakpoint-row"
                  key={line}
                  onClick={() => editorRef.current?.revealLineInCenter(line)}
                >
                  <span className="red-dot" />
                  {file.name}
                  <span>:{line}</span>
                  <X
                    size={12}
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleBreakpoint(line);
                    }}
                  />
                </button>
              ))
          ) : (
            <p className="breakpoint-help">
              Clique ao lado do número da linha
              <br />
              para pausar onde importa.
            </p>
          )}
          <div className="debug-tip">
            <Bug size={17} />
            <span>
              Entenda cada passo
              <br />
              <small>Depure no seu ritmo.</small>
            </span>
          </div>
        </aside>
      </div>
      <footer className="statusbar">
        <div>
          <span className="connection">
            <Check size={12} /> Tudo pronto
          </span>
          <span>Algora 0.1</span>
        </div>
        <div>
          <span>
            Ln {cursor.lineNumber}, Col {cursor.column}
          </span>
          <span>UTF-8</span>
          <span>Portugol · VisualG</span>
          <Braces size={13} />
        </div>
      </footer>
    </div>
  );
}
