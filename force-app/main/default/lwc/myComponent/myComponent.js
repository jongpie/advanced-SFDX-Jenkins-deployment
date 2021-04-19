import { LightningElement } from "lwc";

export default class RollupForceRecalculation extends LightningElement {
    _hasRendered = false;

    async renderedCallback() {
        if (!this._hasRendered) {
            document.title = "My Component";
            this._hasRendered = true;
        }
    }
}
