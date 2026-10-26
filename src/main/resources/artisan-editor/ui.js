import { createModuleI18n as W, fieldText as Y, newTextKey as _, textValue as y, injectModuleStyles as J, useProject as U, useContentLang as G, Button as P, selectDocumentItem as Q, ActionChainEditor as X, updateDocument as Z, SortableList as ee, MiniMessageInput as te, ItemInput as ne, SDK_VERSION as ie } from "@artisan/editor-sdk";
import { jsxs as h, jsx as s, Fragment as A } from "react/jsx-runtime";
import { useState as oe, useEffect as re } from "react";
const q = {
  kindLabel: "Store categories",
  collection: "Catalog",
  category: "Category",
  name: "Name",
  icon: "Icon",
  iconHint: "What the main menu shows for this category.",
  discount: "Discount (%)",
  categoryDiscountHint: "Taken off every article of the category, added to the store's and the article's.",
  order: "Position in the store",
  orderHint: "Lower first; empty: after the others, by id.",
  items: "Articles",
  addItem: "New article",
  newItemName: "New article",
  noItems: "No article yet.",
  open: "Edit",
  remove: "Remove",
  back: "Back to the category",
  item: "Article",
  id: "Id",
  idHint: "Names the article in purchases: unique across the store.",
  idTaken: "Another article of the store has this id: a purchase could buy the wrong one.",
  itemIcon: "Icon",
  itemIconHint: "What the player sees in the category's menu: item, name, lore.",
  price: "Price",
  itemDiscountHint: "Taken off this article only.",
  paid: "The player pays {{price}}",
  actions: "After payment",
  actionsHint: "What the purchase gives: commands, items, messages…",
  kept: "Kept as is: {{fields}}",
  itemCount: "{{count}} articles",
  itemCountOne: "{{count}} article",
  hoverPrice: "Price: {{price}}",
  hoverPaid: "Paid: {{price}} (-{{percent}}%)"
}, k = {
  kindLabel: "Catégories de boutique",
  collection: "Catalogue",
  category: "Catégorie",
  name: "Nom",
  icon: "Icône",
  iconHint: "Ce que le menu principal affiche pour cette catégorie.",
  discount: "Remise (%)",
  categoryDiscountHint: "Retirée de chaque article de la catégorie, en plus de celle de la boutique et de l'article.",
  order: "Position dans la boutique",
  orderHint: "Les plus petites d'abord ; vide : après les autres, par id.",
  items: "Articles",
  addItem: "Nouvel article",
  newItemName: "Nouvel article",
  noItems: "Aucun article pour l'instant.",
  open: "Modifier",
  remove: "Supprimer",
  back: "Retour à la catégorie",
  item: "Article",
  id: "Id",
  idHint: "Nomme l'article dans les achats : unique dans toute la boutique.",
  idTaken: "Un autre article de la boutique a déjà cet id : un achat pourrait acheter le mauvais.",
  itemIcon: "Icône",
  itemIconHint: "Ce que voit le joueur dans le menu de la catégorie : item, nom, lore.",
  price: "Prix",
  itemDiscountHint: "Retirée de cet article seulement.",
  paid: "Le joueur paie {{price}}",
  actions: "Après le paiement",
  actionsHint: "Ce que l'achat donne : commandes, items, messages…",
  kept: "Conservé tel quel : {{fields}}",
  itemCount: "{{count}} articles",
  itemCountOne: "{{count}} article",
  hoverPrice: "Prix : {{price}}",
  hoverPaid: "Payé : {{price}} (-{{percent}} %)"
}, j = { en: q.kindLabel, fr: k.kindLabel }, se = { en: q.collection, fr: k.collection }, ae = W({ en: q, fr: k });
function $(e) {
  const t = e === "fr" ? k : q;
  return (n, o) => {
    const i = t[n] ?? q[n] ?? n;
    return o ? i.replace(/\{\{(\w+)\}\}/g, (a, r) => String(o[r] ?? a)) : i;
  };
}
const I = "boutique_category", O = "catalogue", L = "boutique_category/v1", ce = "boutique_addon";
function C(e) {
  return e && typeof e == "object" && !Array.isArray(e) ? { ...e } : {};
}
function le(e) {
  return Array.isArray(e) ? e : [];
}
function x(e) {
  return typeof e == "string" || typeof e == "number" || typeof e == "boolean" ? String(e) : "";
}
function g(e, t = 0) {
  return typeof e == "number" && Number.isFinite(e) ? e : typeof e == "string" && e.trim() !== "" && Number.isFinite(Number(e)) ? Number(e) : t;
}
function v(e, ...t) {
  const n = { ...e };
  for (const o of t) delete n[o];
  return n;
}
function de(e) {
  return e.toLowerCase().replace(/[^a-z0-9_]/g, "_");
}
function ue(e, t) {
  const n = new Set(t);
  if (!n.has(e)) return e;
  for (let o = 2; ; o++) if (!n.has(`${e}_${o}`)) return `${e}_${o}`;
}
function w(e, ...t) {
  const n = t.reduce((o, i) => o + i, 0);
  return Math.round(Math.max(0, e * (1 - n / 100)));
}
function me(e, t) {
  const n = {}, o = [];
  function i(c) {
    if (c && typeof c == "object" && !Array.isArray(c)) {
      const m = _(), u = {};
      for (const [f, b] of Object.entries(c)) u[f] = x(b);
      return n[m] = u, { key: m };
    }
    const p = typeof c == "string" ? /^\{i18n:(.+)\}$/.exec(c) : null;
    if (p) {
      const m = t.translations?.[p[1]];
      return m && (n[p[1]] = { ...m }), { key: p[1] };
    }
    const l = _();
    return n[l] = c == null || c === "" ? {} : { [t.lang]: x(c) }, { key: l };
  }
  const a = le(e.items).map((c, p) => {
    const l = C(c);
    return {
      id: x(l.id) || `item_${p + 1}`,
      name: i(l.name),
      icon: l.icon ?? "",
      price: g(l.price),
      discount: g(l.discount),
      actions: l.actions ?? null,
      rest: v(l, "id", "name", "icon", "price", "discount", "actions")
    };
  }), r = e.order == null ? void 0 : g(e.order, Number.NaN);
  return { doc: {
    id: x(e.id),
    projectId: t.projectId,
    name: i(e.name),
    icon: e.icon ?? "",
    discount: g(e.discount),
    order: r === void 0 || Number.isNaN(r) ? void 0 : r,
    items: a,
    rest: v(e, "id", "schema", "name", "icon", "discount", "order", "items")
  }, i18nKeys: n, unsupported: o };
}
function pe(e) {
  return e == null ? !0 : typeof e == "string" ? e.trim() === "" : Array.isArray(e) && e.length === 0;
}
function fe(e, t) {
  const n = (i) => Y(t, i.key), o = {
    id: e.id,
    schema: L,
    name: n(e.name),
    icon: e.icon === "" ? void 0 : e.icon,
    discount: e.discount,
    order: e.order,
    ...e.rest,
    items: e.items.map((i) => {
      const a = {
        id: i.id,
        name: n(i.name),
        icon: i.icon === "" ? void 0 : i.icon,
        price: i.price,
        discount: i.discount === 0 ? void 0 : i.discount,
        ...i.rest,
        actions: pe(i.actions) ? void 0 : i.actions
      };
      for (const r of Object.keys(a)) a[r] === void 0 && delete a[r];
      return a;
    })
  };
  for (const i of Object.keys(o)) o[i] === void 0 && delete o[i];
  return o;
}
function* he(e) {
  const t = `${O}.${e.id}`;
  yield { lText: e.name, key: `${t}.name`, path: "name", kind: "boutique_category.name" };
  const n = /* @__PURE__ */ new Set();
  for (let o = 0; o < e.items.length; o++) {
    const i = e.items[o], a = n.has(i.id) ? `${i.id}_${o}` : i.id;
    n.add(i.id), yield {
      lText: i.name,
      key: `${t}.items.${a}.name`,
      path: `items[${o}].name`,
      kind: "boutique_item.name"
    };
  }
}
function be(e, t) {
  if (typeof e == "string") return e ? { material: e } : {};
  const n = C(e), o = n.extends ?? n.item ?? n.material, i = v(n, "extends", "item", "material");
  o !== void 0 && (i.material = typeof o == "string" ? o : "");
  for (const a of ["title", "lore"]) {
    const r = i[a];
    if (r && typeof r == "object" && !Array.isArray(r)) {
      const d = r;
      i[a] = d[t] ?? Object.values(d)[0];
    }
  }
  return i;
}
function ge(e) {
  const t = typeof e.material == "string" ? e.material : "", n = v(e, "material");
  for (const o of Object.keys(n)) {
    const i = n[o];
    (i === void 0 || i === "" || Array.isArray(i) && i.length === 0) && delete n[o];
  }
  return Object.keys(n).length === 0 ? t : { extends: t, ...n };
}
function E(e) {
  if (typeof e == "string") return e;
  const t = C(e), n = t.extends ?? t.item ?? t.material;
  return n === void 0 ? "" : E(n);
}
function H(e) {
  const t = /* @__PURE__ */ new Set();
  return e.items.map((n, o) => {
    const i = t.has(n.id) ? `${n.id}#${o}` : n.id;
    return t.add(n.id), i;
  });
}
const ye = "#d97706";
function z(e) {
  const t = e?.dataSources?.find(
    (n) => String(n.id) === "shop_settings"
  );
  return C(t?.value);
}
function S(e) {
  return g(z(e).whole_shop_discount);
}
function D(e) {
  return e.replace(/<[^<>]*>/g, "").replace(/[&§]#[0-9a-fA-F]{6}/g, "").replace(/#[0-9a-fA-F]{6}/g, "").replace(/[&§][0-9a-fk-orx]/gi, "").trim();
}
function R(e, t) {
  const n = Math.round(e);
  if (t >= n) return String(n);
  const o = n <= 0 ? 0 : Math.round((n - t) * 100 / n);
  return `${t} (-${o}%)`;
}
function qe(e, t, n) {
  const o = $(n), i = S(t), a = H(e);
  return e.items.map((r, d) => {
    const c = a[d], p = D(y(t, r.name.key, n)) || r.id, l = w(r.price, i, e.discount, r.discount), m = [o("hoverPrice", { price: String(Math.round(r.price)) })], u = i + e.discount + r.discount;
    return u > 0 && m.push(o("hoverPaid", { price: String(l), percent: String(u) })), {
      id: c,
      label: p,
      detail: R(r.price, l),
      icon: r.icon,
      hover: { title: p, lines: m }
    };
  });
}
function xe(e, t) {
  const o = $(t)(e.items.length === 1 ? "itemCountOne" : "itemCount", {
    count: String(e.items.length)
  });
  return e.discount > 0 ? `${o} · -${e.discount}%` : o;
}
const ve = `
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
function Ne({ projectId: e, documentId: t, document: n, item: o }) {
  J("boutique", ve);
  const i = n, a = U(e), r = G(), d = ae(), [c, p] = oe(o ?? null);
  re(() => {
    p(o ?? null);
  }, [o]);
  const l = (b) => {
    p(b), Q({ documentId: t, item: b });
  }, u = { projectId: e, project: a, lang: r, t: d, edit: (b, B) => {
    const M = structuredClone(i);
    b(M), Z({ projectId: e, kind: I, id: t, doc: M, texts: B });
  } }, f = c ? H(i).indexOf(c) : -1;
  return f >= 0 ? /* @__PURE__ */ h("div", { className: "bq", children: [
    /* @__PURE__ */ h(P, { variant: "ghost", size: "sm", className: "bq-back", onClick: () => l(null), children: [
      "← ",
      d("back")
    ] }),
    /* @__PURE__ */ s(
      we,
      {
        ctx: u,
        category: i,
        index: f,
        onIdChange: (b) => p(b)
      }
    )
  ] }) : /* @__PURE__ */ s("div", { className: "bq", children: /* @__PURE__ */ s(Ce, { ctx: u, category: i, onOpen: l }) });
}
function T({
  label: e,
  children: t,
  hint: n
}) {
  return (
    // biome-ignore lint/a11y/noLabelWithoutControl: the control is the child
    /* @__PURE__ */ h("label", { className: "bq-field", children: [
      /* @__PURE__ */ s("span", { className: "field-label", children: e }),
      t,
      n
    ] })
  );
}
function F({ ctx: e, label: t, node: n }) {
  const o = y(e.project, n.key, e.lang), i = (a) => e.edit(() => {
  }, [{ key: n.key, lang: e.lang, value: a }]);
  return /* @__PURE__ */ s(T, { label: `${t} · ${e.lang}`, children: /* @__PURE__ */ s(te, { value: o, onChange: i, "aria-label": t }) });
}
function N({
  label: e,
  value: t,
  onChange: n,
  hint: o,
  max: i,
  optional: a,
  integer: r
}) {
  return /* @__PURE__ */ s(T, { label: e, hint: o ? /* @__PURE__ */ s("span", { className: "bq-hint", children: o }) : void 0, children: /* @__PURE__ */ s(
    "input",
    {
      className: "input",
      type: "number",
      min: 0,
      max: i,
      step: r ? 1 : "any",
      value: t === void 0 ? "" : String(t),
      onChange: (d) => {
        const c = d.target.value.trim();
        if (c === "") return n(a ? void 0 : 0);
        const p = Math.max(0, r ? Math.round(Number(c)) : Number(c));
        Number.isFinite(p) && n(i === void 0 ? p : Math.min(i, p));
      }
    }
  ) });
}
function K({
  ctx: e,
  label: t,
  hint: n,
  value: o,
  onChange: i
}) {
  return /* @__PURE__ */ h("div", { className: "bq-field", children: [
    /* @__PURE__ */ s("span", { className: "field-label", children: t }),
    /* @__PURE__ */ s(
      ne,
      {
        projectId: e.projectId,
        value: be(o, e.lang),
        onChange: (a) => i(ge(a))
      }
    ),
    /* @__PURE__ */ s("span", { className: "bq-hint", children: n })
  ] });
}
function ke({
  label: e,
  onClick: t,
  disabled: n,
  children: o
}) {
  return /* @__PURE__ */ s(
    "button",
    {
      type: "button",
      className: "bq-icon-btn",
      title: e,
      "aria-label": e,
      onClick: t,
      disabled: n,
      children: o
    }
  );
}
function Ie(e, t, n) {
  if (n < 0 || n >= e.length) return;
  const [o] = e.splice(t, 1);
  e.splice(n, 0, o);
}
function V(e, t, n) {
  const o = /* @__PURE__ */ new Set(), i = e?.documents?.[I];
  for (const a of i ?? []) {
    const r = a;
    if (r.id !== t.id)
      for (const d of r.items ?? []) o.add(d.id);
  }
  return t.items.forEach((a, r) => {
    r !== n && o.add(a.id);
  }), o;
}
function Ce({
  ctx: e,
  category: t,
  onOpen: n
}) {
  const { t: o, edit: i, project: a, lang: r } = e, d = S(a), c = H(t), p = () => {
    const l = _(), m = ue("item", V(a, t, -1));
    i(
      (u) => {
        u.items.push({
          id: m,
          name: { key: l },
          icon: "CHEST",
          price: 0,
          discount: 0,
          actions: null,
          rest: {}
        });
      },
      [{ key: l, lang: r, value: o("newItemName") }]
    ), n(m);
  };
  return /* @__PURE__ */ h(A, { children: [
    /* @__PURE__ */ h("section", { className: "bq-section", children: [
      /* @__PURE__ */ s("span", { className: "bq-h", children: o("category") }),
      /* @__PURE__ */ s(F, { ctx: e, label: o("name"), node: t.name }),
      /* @__PURE__ */ s(
        K,
        {
          ctx: e,
          label: o("icon"),
          hint: o("iconHint"),
          value: t.icon,
          onChange: (l) => i((m) => {
            m.icon = l;
          })
        }
      ),
      /* @__PURE__ */ h("div", { className: "bq-row", children: [
        /* @__PURE__ */ s(
          N,
          {
            label: o("discount"),
            hint: o("categoryDiscountHint"),
            max: 100,
            integer: !0,
            value: t.discount,
            onChange: (l) => i((m) => {
              m.discount = l ?? 0;
            })
          }
        ),
        /* @__PURE__ */ s(
          N,
          {
            label: o("order"),
            hint: o("orderHint"),
            optional: !0,
            integer: !0,
            value: t.order,
            onChange: (l) => i((m) => {
              m.order = l;
            })
          }
        )
      ] })
    ] }),
    /* @__PURE__ */ h("section", { className: "bq-section", children: [
      /* @__PURE__ */ s("div", { className: "bq-head", children: /* @__PURE__ */ s("span", { className: "bq-h", children: o("items") }) }),
      t.items.length === 0 ? /* @__PURE__ */ s("span", { className: "bq-hint", children: o("noItems") }) : null,
      /* @__PURE__ */ s(
        ee,
        {
          ids: c,
          onMove: (l, m) => i((u) => Ie(u.items, l, m)),
          className: "bq-list",
          itemClassName: "bq-item",
          children: (l, m) => {
            const u = t.items[l], f = w(u.price, d, t.discount, u.discount);
            return /* @__PURE__ */ h(A, { children: [
              m,
              /* @__PURE__ */ h("button", { type: "button", className: "bq-item-main", onClick: () => n(c[l]), children: [
                /* @__PURE__ */ s("span", { className: "bq-item-name", children: y(a, u.name.key, r) || u.id }),
                /* @__PURE__ */ s("span", { className: "bq-item-mat", children: E(u.icon) }),
                /* @__PURE__ */ s("span", { className: "bq-price", children: R(u.price, f) })
              ] }),
              /* @__PURE__ */ s(
                ke,
                {
                  label: o("remove"),
                  onClick: () => i((b) => {
                    b.items.splice(l, 1);
                  }),
                  children: "×"
                }
              )
            ] });
          }
        }
      ),
      /* @__PURE__ */ h(P, { variant: "secondary", size: "sm", className: "bq-add", onClick: p, children: [
        "+ ",
        o("addItem")
      ] })
    ] })
  ] });
}
function we({
  ctx: e,
  category: t,
  index: n,
  onIdChange: o
}) {
  const { t: i, edit: a, project: r } = e, d = t.items[n], c = (u) => a((f) => {
    u(f.items[n]);
  }), p = V(r, t, n).has(d.id), l = w(d.price, S(r), t.discount, d.discount), m = Object.keys(d.rest);
  return /* @__PURE__ */ h(A, { children: [
    /* @__PURE__ */ h("section", { className: "bq-section", children: [
      /* @__PURE__ */ s("span", { className: "bq-h", children: i("item") }),
      /* @__PURE__ */ s(F, { ctx: e, label: i("name"), node: d.name }),
      /* @__PURE__ */ s(
        T,
        {
          label: i("id"),
          hint: p ? /* @__PURE__ */ s("span", { className: "bq-err", children: i("idTaken") }) : /* @__PURE__ */ s("span", { className: "bq-hint", children: i("idHint") }),
          children: /* @__PURE__ */ s(
            "input",
            {
              className: "input bq-mono",
              value: d.id,
              onChange: (u) => {
                const f = de(u.target.value);
                c((b) => {
                  b.id = f;
                }), o(f);
              }
            }
          )
        }
      ),
      /* @__PURE__ */ s(
        K,
        {
          ctx: e,
          label: i("itemIcon"),
          hint: i("itemIconHint"),
          value: d.icon,
          onChange: (u) => c((f) => {
            f.icon = u;
          })
        }
      ),
      /* @__PURE__ */ h("div", { className: "bq-row", children: [
        /* @__PURE__ */ s(
          N,
          {
            label: i("price"),
            value: d.price,
            onChange: (u) => c((f) => {
              f.price = u ?? 0;
            })
          }
        ),
        /* @__PURE__ */ s(
          N,
          {
            label: i("discount"),
            hint: i("itemDiscountHint"),
            max: 100,
            integer: !0,
            value: d.discount,
            onChange: (u) => c((f) => {
              f.discount = u ?? 0;
            })
          }
        )
      ] }),
      /* @__PURE__ */ s("span", { className: "bq-ok", children: i("paid", { price: String(l) }) })
    ] }),
    /* @__PURE__ */ h("section", { className: "bq-section", children: [
      /* @__PURE__ */ s("span", { className: "bq-h", children: i("actions") }),
      /* @__PURE__ */ s("span", { className: "bq-hint", children: i("actionsHint") }),
      /* @__PURE__ */ s(
        X,
        {
          projectId: e.projectId,
          value: d.actions ?? [],
          onChange: (u) => c((f) => {
            f.actions = u;
          })
        }
      )
    ] }),
    m.length > 0 ? /* @__PURE__ */ s("span", { className: "bq-hint", children: i("kept", { fields: m.join(", ") }) }) : null
  ] });
}
const Se = {
  kind: I,
  dir: O,
  schemaVersion: L,
  // A category's id names it in the menus (`category` of shop_category): typed by hand.
  projects: [ce],
  // The catalogue (SDK 1.9): one entry and one page, off the board, its settings on top.
  // The categories stay in catalogue/ at the folder's root, the only place read.
  collection: { labels: se },
  label: j.en,
  labels: j,
  component: Ne,
  title: (e, { project: t, lang: n }) => D(y(t, e.name.key, n)),
  create: ({ id: e, projectId: t }) => ({
    id: e,
    projectId: t,
    // An empty key: the host mints it at its canonical address.
    name: { key: "" },
    icon: "CHEST",
    discount: 0,
    order: void 0,
    items: [],
    rest: {}
  }),
  parse: (e, t) => me(e, t),
  serialize: (e, t) => fe(e, t),
  ltextSites: (e) => he(e),
  board: {
    size: { width: 260, height: 48 },
    accent: ye,
    items: (e, { project: t, lang: n }) => qe(e, t, n),
    summary: (e, { lang: t }) => xe(e, t)
  }
};
function _e(e, t, n) {
  const o = n.placeholders, i = t < Math.round(e), a = o?.[i ? "discount_price_display" : "price_display"], r = typeof a == "string" && a.trim() ? a : i ? "&m%old_price% &f%price% tokens (-%discount_percent%%)" : "%price% tokens", d = Math.round(e);
  return r.replaceAll("%old_price%", String(d)).replaceAll("%discount_percent%", String(d <= 0 ? 0 : Math.round((d - t) * 100 / d))).replaceAll("%price%", String(t));
}
function Ae(e) {
  return [...e].sort((t, n) => {
    const o = t.order ?? Number.POSITIVE_INFINITY, i = n.order ?? Number.POSITIVE_INFINITY;
    return o !== i ? o - i : t.id.localeCompare(n.id);
  });
}
function He(e, t) {
  if (t !== "boutique:categories" && t !== "boutique:items") return null;
  const n = e.documents?.[I];
  if (!n || n.length === 0) return null;
  const o = e.defaultLang ?? "fr", i = z(e), a = S(e), r = Ae(n), d = (c) => c.items.map((p) => {
    const l = g(p.price), m = w(l, a, c.discount, p.discount);
    return {
      id: p.id,
      name: y(e, p.name.key, o),
      icon: p.icon,
      category: c.id,
      price: l,
      final_price: m,
      price_display: _e(l, m, i),
      lore: ""
    };
  });
  return t === "boutique:items" ? r.flatMap(d) : r.map((c) => ({
    id: c.id,
    name: y(e, c.name.key, o),
    icon: c.icon,
    items: d(c)
  }));
}
const Te = {
  id: "boutique",
  displayName: "Store",
  schemaVersion: "boutique/v1",
  sdk: ie,
  sidebar: { icon: () => null, label: "Store", to: "/m/boutique" },
  // Categories are Documents of the folder; the settings are a screen the plugin declares.
  routes: [],
  documents: [Se],
  localPreviewRows: He,
  dataSourceModels: {
    "boutique:items": "boutique:item",
    "boutique:categories": "boutique:category"
  }
}, $e = { ...Te, sdk: "1.10.0" };
export {
  $e as default
};
