const vscode = require('vscode');

const COMPONENTS = [
    {
        label: 'VSRC',
        detail: 'Voltage source',
        documentation: 'Sinusoidal or DC voltage source.\n\nSyntax: `VSRC <name> <node+> <node-> type=sin|dc val=<V> freq=<Hz>`',
        insertText: 'VSRC ${1:V1} ${2:N1} ${3:0} type=${4|sin,dc|} val=${5:10.0} freq=${6:50}',
        kind: vscode.CompletionItemKind.Module,
    },
    {
        label: 'ISRC',
        detail: 'Current source',
        documentation: 'Sinusoidal or DC current source.\n\nSyntax: `ISRC <name> <node+> <node-> type=sin|dc val=<A> freq=<Hz>`',
        insertText: 'ISRC ${1:I1} ${2:N1} ${3:0} type=${4|sin,dc|} val=${5:1.0} freq=${6:50}',
        kind: vscode.CompletionItemKind.Module,
    },
    {
        label: 'RES',
        detail: 'Resistor',
        documentation: 'Two-terminal resistor.\n\nSyntax: `RES <name> <node1> <node2> val=<Ohm>`',
        insertText: 'RES ${1:R1} ${2:N1} ${3:N2} val=${4:1000}',
        kind: vscode.CompletionItemKind.Module,
    },
    {
        label: 'CAP',
        detail: 'Capacitor',
        documentation: 'Two-terminal capacitor.\n\nSyntax: `CAP <name> <node1> <node2> val=<F>`',
        insertText: 'CAP ${1:C1} ${2:N1} ${3:0} val=${4:1e-6}',
        kind: vscode.CompletionItemKind.Module,
    },
    {
        label: 'IND',
        detail: 'Inductor',
        documentation: 'Two-terminal inductor.\n\nSyntax: `IND <name> <node1> <node2> val=<H>`',
        insertText: 'IND ${1:L1} ${2:N1} ${3:N2} val=${4:1e-3}',
        kind: vscode.CompletionItemKind.Module,
    },
];

const DIRECTIVES = [
    {
        label: '.SIMULATE',
        detail: 'Simulation directive',
        documentation:
            'Run a simulation.\n\nSyntax: `.SIMULATE type=trans tstop=<seconds> tstep=<seconds>`\n\n' +
            'Parameters:\n' +
            '- `type`  – simulation type (`trans` = transient)\n' +
            '- `tstop` – end time [s]\n' +
            '- `tstep` – time step [s]',
        insertText: '.SIMULATE type=${1|trans|} tstop=${2:0.04} tstep=${3:0.0001}',
        kind: vscode.CompletionItemKind.Keyword,
    },
];

const PARAM_KEYS = {
    VSRC: [
        { label: 'type=', detail: 'Source waveform type', insertText: 'type=${1|sin,dc|}' },
        { label: 'val=', detail: 'Amplitude [V]', insertText: 'val=${1:10.0}' },
        { label: 'freq=', detail: 'Frequency [Hz]', insertText: 'freq=${1:50}' },
    ],
    ISRC: [
        { label: 'type=', detail: 'Source waveform type', insertText: 'type=${1|sin,dc|}' },
        { label: 'val=', detail: 'Amplitude [A]', insertText: 'val=${1:1.0}' },
        { label: 'freq=', detail: 'Frequency [Hz]', insertText: 'freq=${1:50}' },
    ],
    RES: [
        { label: 'val=', detail: 'Resistance [Ω]', insertText: 'val=${1:1000}' },
    ],
    CAP: [
        { label: 'val=', detail: 'Capacitance [F]', insertText: 'val=${1:1e-6}' },
    ],
    IND: [
        { label: 'val=', detail: 'Inductance [H]', insertText: 'val=${1:1e-3}' },
    ],
    '.SIMULATE': [
        { label: 'type=', detail: 'Simulation type', insertText: 'type=${1|trans|}' },
        { label: 'tstop=', detail: 'End time [s]', insertText: 'tstop=${1:0.04}' },
        { label: 'tstep=', detail: 'Time step [s]', insertText: 'tstep=${1:0.0001}' },
    ],
};

const TYPE_VALUES = [
    { label: 'sin', detail: 'Sinusoidal waveform' },
    { label: 'dc', detail: 'DC (constant) waveform' },
    { label: 'trans', detail: 'Transient analysis' },
];

function collectNodes(document) {
    const nodes = new Set();
    const componentRe = /^\s*(?:VSRC|ISRC|RES|CAP|IND)\s+\S+\s+(\S+)\s+(\S+)/;
    for (let i = 0; i < document.lineCount; i++) {
        const m = document.lineAt(i).text.match(componentRe);
        if (m) {
            nodes.add(m[1]);
            nodes.add(m[2]);
        }
    }
    return nodes;
}

function lineComponent(lineText) {
    const m = lineText.match(/^\s*(VSRC|ISRC|RES|CAP|IND|\.SIMULATE)\b/);
    return m ? m[1] : null;
}

function activate(context) {
    const provider = vscode.languages.registerCompletionItemProvider(
        { language: 'bcs', scheme: 'file' },
        {
            provideCompletionItems(document, position) {
                const line = document.lineAt(position).text;
                const prefix = line.substring(0, position.character);
                const items = [];

                if (/type=\w*$/.test(prefix)) {
                    for (const v of TYPE_VALUES) {
                        const item = new vscode.CompletionItem(v.label, vscode.CompletionItemKind.EnumMember);
                        item.detail = v.detail;
                        items.push(item);
                    }
                    return items;
                }

                const comp = lineComponent(prefix);
                if (comp && /\s$/.test(prefix)) {
                    const tokens = prefix.trim().split(/\s+/);
                    const minTokens = comp.startsWith('.') ? 1 : 4;
                    if (tokens.length >= minTokens) {
                        const keys = PARAM_KEYS[comp] || [];
                        for (const k of keys) {
                            const item = new vscode.CompletionItem(k.label, vscode.CompletionItemKind.Property);
                            item.detail = k.detail;
                            item.insertText = new vscode.SnippetString(k.insertText);
                            items.push(item);
                        }
                    }

                    if (!comp.startsWith('.') && tokens.length >= 2 && tokens.length <= 4) {
                        const nodes = collectNodes(document);
                        for (const n of nodes) {
                            const item = new vscode.CompletionItem(n, vscode.CompletionItemKind.Variable);
                            item.detail = n === '0' ? 'Ground' : 'Node';
                            items.push(item);
                        }
                    }
                    if (items.length > 0) return items;
                }

                if (/^\s*\S*$/.test(prefix)) {
                    for (const c of COMPONENTS) {
                        const item = new vscode.CompletionItem(c.label, c.kind);
                        item.detail = c.detail;
                        item.documentation = new vscode.MarkdownString(c.documentation);
                        item.insertText = new vscode.SnippetString(c.insertText);
                        items.push(item);
                    }
                    for (const d of DIRECTIVES) {
                        const item = new vscode.CompletionItem(d.label, d.kind);
                        item.detail = d.detail;
                        item.documentation = new vscode.MarkdownString(d.documentation);
                        item.insertText = new vscode.SnippetString(d.insertText);
                        items.push(item);
                    }

                    const commentItem = new vscode.CompletionItem('* comment', vscode.CompletionItemKind.Snippet);
                    commentItem.detail = 'Line comment';
                    commentItem.insertText = new vscode.SnippetString('* ${1:description}');
                    items.push(commentItem);

                    return items;
                }

                return items;
            },
        },
        '.', // trigger on "." for directives
        '=', // trigger on "=" for parameter values
    );

    const hoverProvider = vscode.languages.registerHoverProvider(
        { language: 'bcs', scheme: 'file' },
        {
            provideHover(document, position) {
                const range = document.getWordRangeAtPosition(position, /[A-Z_]+/);
                if (!range) return null;
                const word = document.getText(range);

                const comp = COMPONENTS.find((c) => c.label === word);
                if (comp) {
                    return new vscode.Hover(new vscode.MarkdownString(comp.documentation));
                }

                const dir = DIRECTIVES.find((d) => d.label === '.' + word || d.label === word);
                if (dir) {
                    return new vscode.Hover(new vscode.MarkdownString(dir.documentation));
                }

                const paramHovers = {
                    val: 'Component value (resistance in Ω, capacitance in F, inductance in H, voltage in V, current in A)',
                    freq: 'Signal frequency in Hz',
                    type: 'Waveform or simulation type (`sin`, `dc`, `trans`)',
                    tstop: 'Simulation end time in seconds',
                    tstep: 'Simulation time step in seconds',
                };
                const lowerWord = word.toLowerCase();
                if (paramHovers[lowerWord]) {
                    return new vscode.Hover(new vscode.MarkdownString(`**${lowerWord}** — ${paramHovers[lowerWord]}`));
                }

                return null;
            },
        }
    );

    context.subscriptions.push(provider, hoverProvider);
}

function deactivate() { }

module.exports = { activate, deactivate };
