export class AlgoraError extends Error {
  constructor(
    message: string,
    public line: number,
  ) {
    super(`Linha ${line}: ${message}`);
    this.name = "AlgoraError";
  }
}
export const norm = (s: string) =>
  s
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
