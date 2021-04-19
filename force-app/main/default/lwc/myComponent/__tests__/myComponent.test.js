import { createElement } from "lwc";

// import { getObjectInfo } from 'lightning/uiObjectInfoApi';
// import performFullRecalculation from '@salesforce/apex/Rollup.performFullRecalculation';
// import performBulkFullRecalc from '@salesforce/apex/Rollup.performBulkFullRecalc';
import MyComponent from "c/myComponent";
// import { registerLdsTestWireAdapter } from '@salesforce/sfdx-lwc-jest';

// const mockGetObjectInfo = require('./data/rollupCMDTWireAdapter.json');
// const getObjectInfoWireAdapter = registerLdsTestWireAdapter(getObjectInfo);

async function assertForTestConditions() {
    const resolvedPromise = Promise.resolve();
    return resolvedPromise.then.apply(resolvedPromise, arguments);
}

describe("Example LWC tests", () => {
    afterEach(() => {
        while (document.body.firstChild) {
            document.body.removeChild(document.body.firstChild);
        }
        jest.clearAllMocks();
    });

    it("sets document title", async () => {
        const myLwcComponent = createElement("c-my-component", {
            is: MyComponent
        });
        document.body.appendChild(myLwcComponent);

        return assertForTestConditions(() => {
            expect(document.title).toEqual("My Component");
        });
    });
});
