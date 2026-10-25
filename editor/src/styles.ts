/**
 * The view's own styles (`bq-` prefix): a bundle loaded at runtime cannot count on the
 * editor's Tailwind utilities, only on its design-system classes and tokens.
 */
export const SHOP_CSS = `
.bq { display: flex; flex-direction: column; gap: 14px; padding: 12px; color: var(--text); font-size: 12.5px; }
.bq .input { height: 32px; padding: 5px 10px; font-size: 12.5px; }
.bq-section { display: flex; flex-direction: column; gap: 8px; padding-top: 12px; border-top: 1px solid var(--border-soft); }
.bq-section:first-child { border-top: 0; padding-top: 0; }
.bq-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.bq-h { font-size: 11px; font-weight: 600; letter-spacing: .04em; text-transform: uppercase; color: var(--text-mut); }
.bq-field { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.bq-row { display: flex; gap: 8px; align-items: flex-start; flex-wrap: wrap; }
.bq-row > * { flex: 1 1 120px; min-width: 0; }
.bq-hint { color: var(--text-faint); font-size: 11.5px; }
.bq-err { color: var(--danger); font-size: 11.5px; }
.bq-ok { color: var(--success); font-size: 11.5px; }
.bq-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
.bq-item { display: flex; align-items: center; gap: 6px; padding: 4px 6px 4px 2px; border: 1px solid var(--border); border-radius: var(--r-md); background: var(--surface); }
.bq-item-main { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; cursor: pointer; background: none; border: 0; color: inherit; text-align: left; padding: 3px 0; font: inherit; }
.bq-item-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
.bq-item-mat { color: var(--text-faint); font-size: 10.5px; font-family: var(--font-mono); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 40%; }
.bq-price { flex: none; padding: 1px 8px; border-radius: 999px; background: var(--brand-soft); color: var(--text); font-size: 11px; font-family: var(--font-mono); }
.bq-off { flex: none; padding: 1px 6px; border-radius: 999px; background: var(--surface-inset); color: var(--success); font-size: 10.5px; font-family: var(--font-mono); }
.bq-icon-btn { width: 24px; height: 24px; display: inline-flex; align-items: center; justify-content: center; border-radius: var(--r-sm); border: 0; background: none; color: var(--text-faint); cursor: pointer; flex: none; }
.bq-icon-btn:hover:not(:disabled) { background: var(--surface-hover); color: var(--text); }
.bq-icon-btn:disabled { opacity: .35; cursor: default; }
.bq-back { align-self: flex-start; }
.bq-add { align-self: flex-start; }
.bq-mono { font-family: var(--font-mono); }
`;
