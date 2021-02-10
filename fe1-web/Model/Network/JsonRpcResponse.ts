import { Verifiable } from "./Verifiable";

export abstract class JsonRpcResponse implements Verifiable {

    public readonly id: number;

    constructor(resp: Partial<JsonRpcResponse>) {
        this.id = resp.id || -1;
    }

    verify(): boolean {
        throw new Error("Method not implemented.");
    }
}
