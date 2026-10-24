import { createModuleI18n as Y, fieldText as J, newTextKey as A, textValue as y, injectModuleStyles as G, useProject as Q, useContentLang as X, Button as $, selectDocumentItem as Z, ActionChainEditor as ee, updateDocument as te, MiniMessageInput as ne, ItemInput as ie, SDK_VERSION as oe } from "@artisan/editor-sdk";
import { jsxs as h, jsx as s, Fragment as O } from "react/jsx-runtime";
import { useState as re, useEffect as se } from "react";
const q = {
  kindLabel: "Shop categories",
  collection: "Catalog",
  category: "Category",
  name: "Name",
  icon: "Icon",
  iconHint: "What the main menu shows for this category.",
  discount: "Discount (%)",
  categoryDiscountHint: "Taken off every article of the category, added to the shop's and the article's.",
  order: "Position in the shop",
  orderHint: "Lower first; empty: after the others, by id.",
  items: "Articles",
  addItem: "New article",
  newItemName: "New article",
  noItems: "No article yet.",
  open: "Edit",
  remove: "Remove",
  moveUp: "Move up",
  moveDown: "Move down",
  back: "Back to the category",
  item: "Article",
  id: "Id",
  idHint: "Names the article in purchases: unique across the shop.",
  idTaken: "Another article of the shop has this id: a purchase could buy the wrong one.",
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
  moveUp: "Monter",
  moveDown: "Descendre",
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
}, j = { en: q.kindLabel, fr: k.kindLabel }, ae = { en: q.collection, fr: k.collection }, ce = Y({ en: q, fr: k });
function L(e) {
  const t = e === "fr" ? k : q;
  return (n, o) => {
    const i = t[n] ?? q[n] ?? n;
    return o ? i.replace(/\{\{(\w+)\}\}/g, (a, r) => String(o[r] ?? a)) : i;
  };
}
const w = "boutique_category", E = "boutique_categories", D = "boutique_category/v1", le = "boutique_addon";
function I(e) {
  return e && typeof e == "object" && !Array.isArray(e) ? { ...e } : {};
}
function de(e) {
  return Array.isArray(e) ? e : [];
}
function v(e) {
  return typeof e == "string" || typeof e == "number" || typeof e == "boolean" ? String(e) : "";
}
function g(e, t = 0) {
  return typeof e == "number" && Number.isFinite(e) ? e : typeof e == "string" && e.trim() !== "" && Number.isFinite(Number(e)) ? Number(e) : t;
}
function x(e, ...t) {
  const n = { ...e };
  for (const o of t) delete n[o];
  return n;
}
function ue(e) {
  return e.toLowerCase().replace(/[^a-z0-9_]/g, "_");
}
function me(e, t) {
  const n = new Set(t);
  if (!n.has(e)) return e;
  for (let o = 2; ; o++) if (!n.has(`${e}_${o}`)) return `${e}_${o}`;
}
function C(e, ...t) {
  const n = t.reduce((o, i) => o + i, 0);
  return Math.round(Math.max(0, e * (1 - n / 100)));
}
function pe(e, t) {
  const n = {}, o = [];
  function i(c) {
    if (c && typeof c == "object" && !Array.isArray(c)) {
      const d = A(), p = {};
      for (const [f, b] of Object.entries(c)) p[f] = v(b);
      return n[d] = p, { key: d };
    }
    const m = typeof c == "string" ? /^\{i18n:(.+)\}$/.exec(c) : null;
    if (m) {
      const d = t.translations?.[m[1]];
      return d && (n[m[1]] = { ...d }), { key: m[1] };
    }
    const l = A();
    return n[l] = c == null || c === "" ? {} : { [t.lang]: v(c) }, { key: l };
  }
  const a = de(e.items).map((c, m) => {
    const l = I(c);
    return {
      id: v(l.id) || `item_${m + 1}`,
      name: i(l.name),
      icon: l.icon ?? "",
      price: g(l.price),
      discount: g(l.discount),
      actions: l.actions ?? null,
      rest: x(l, "id", "name", "icon", "price", "discount", "actions")
    };
  }), r = e.order == null ? void 0 : g(e.order, Number.NaN);
  return { doc: {
    id: v(e.id),
    projectId: t.projectId,
    name: i(e.name),
    icon: e.icon ?? "",
    discount: g(e.discount),
    order: r === void 0 || Number.isNaN(r) ? void 0 : r,
    items: a,
    rest: x(e, "id", "schema", "name", "icon", "discount", "order", "items")
  }, i18nKeys: n, unsupported: o };
}
function fe(e) {
  return e == null ? !0 : typeof e == "string" ? e.trim() === "" : Array.isArray(e) && e.length === 0;
}
function he(e, t) {
  const n = (i) => J(t, i.key), o = {
    id: e.id,
    schema: D,
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
        actions: fe(i.actions) ? void 0 : i.actions
      };
      for (const r of Object.keys(a)) a[r] === void 0 && delete a[r];
      return a;
    })
  };
  for (const i of Object.keys(o)) o[i] === void 0 && delete o[i];
  return o;
}
function* be(e) {
  const t = `${E}.${e.id}`;
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
function ge(e, t) {
  if (typeof e == "string") return e ? { material: e } : {};
  const n = I(e), o = n.extends ?? n.item ?? n.material, i = x(n, "extends", "item", "material");
  o !== void 0 && (i.material = typeof o == "string" ? o : "");
  for (const a of ["title", "lore"]) {
    const r = i[a];
    if (r && typeof r == "object" && !Array.isArray(r)) {
      const u = r;
      i[a] = u[t] ?? Object.values(u)[0];
    }
  }
  return i;
}
function ye(e) {
  const t = typeof e.material == "string" ? e.material : "", n = x(e, "material");
  for (const o of Object.keys(n)) {
    const i = n[o];
    (i === void 0 || i === "" || Array.isArray(i) && i.length === 0) && delete n[o];
  }
  return Object.keys(n).length === 0 ? t : { extends: t, ...n };
}
function z(e) {
  if (typeof e == "string") return e;
  const t = I(e), n = t.extends ?? t.item ?? t.material;
  return n === void 0 ? "" : z(n);
}
function H(e) {
  const t = /* @__PURE__ */ new Set();
  return e.items.map((n, o) => {
    const i = t.has(n.id) ? `${n.id}#${o}` : n.id;
    return t.add(n.id), i;
  });
}
const qe = "#d97706";
function R(e) {
  const t = e?.dataSources?.find(
    (n) => String(n.id) === "shop_settings"
  );
  return I(t?.value);
}
function S(e) {
  return g(R(e).whole_shop_discount);
}
function F(e) {
  return e.replace(/<[^<>]*>/g, "").replace(/[&§]#[0-9a-fA-F]{6}/g, "").replace(/#[0-9a-fA-F]{6}/g, "").replace(/[&§][0-9a-fk-orx]/gi, "").trim();
}
function B(e, t) {
  const n = Math.round(e);
  if (t >= n) return String(n);
  const o = n <= 0 ? 0 : Math.round((n - t) * 100 / n);
  return `${t} (-${o}%)`;
}
function ve(e, t, n) {
  const o = L(n), i = S(t), a = H(e);
  return e.items.map((r, u) => {
    const c = a[u], m = F(y(t, r.name.key, n)) || r.id, l = C(r.price, i, e.discount, r.discount), d = [o("hoverPrice", { price: String(Math.round(r.price)) })], p = i + e.discount + r.discount;
    return p > 0 && d.push(o("hoverPaid", { price: String(l), percent: String(p) })), {
      id: c,
      label: m,
      detail: B(r.price, l),
      icon: r.icon,
      hover: { title: m, lines: d }
    };
  });
}
function xe(e, t) {
  const o = L(t)(e.items.length === 1 ? "itemCountOne" : "itemCount", {
    count: String(e.items.length)
  });
  return e.discount > 0 ? `${o} · -${e.discount}%` : o;
}
const Ne = `
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
.bq-item { display: flex; align-items: center; gap: 6px; padding: 4px 6px 4px 8px; border: 1px solid var(--border); border-radius: var(--r-md); background: var(--surface); }
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
function ke({ projectId: e, documentId: t, document: n, item: o }) {
  G("boutique", Ne);
  const i = n, a = Q(e), r = X(), u = ce(), [c, m] = re(o ?? null);
  se(() => {
    m(o ?? null);
  }, [o]);
  const l = (b) => {
    m(b), Z({ documentId: t, item: b });
  }, p = { projectId: e, project: a, lang: r, t: u, edit: (b, W) => {
    const T = structuredClone(i);
    b(T), te({ projectId: e, kind: w, id: t, doc: T, texts: W });
  } }, f = c ? H(i).indexOf(c) : -1;
  return f >= 0 ? /* @__PURE__ */ h("div", { className: "bq", children: [
    /* @__PURE__ */ h($, { variant: "ghost", size: "sm", className: "bq-back", onClick: () => l(null), children: [
      "← ",
      u("back")
    ] }),
    /* @__PURE__ */ s(
      Ie,
      {
        ctx: p,
        category: i,
        index: f,
        onIdChange: (b) => m(b)
      }
    )
  ] }) : /* @__PURE__ */ s("div", { className: "bq", children: /* @__PURE__ */ s(we, { ctx: p, category: i, onOpen: l }) });
}
function M({
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
function K({ ctx: e, label: t, node: n }) {
  const o = y(e.project, n.key, e.lang), i = (a) => e.edit(() => {
  }, [{ key: n.key, lang: e.lang, value: a }]);
  return /* @__PURE__ */ s(M, { label: `${t} · ${e.lang}`, children: /* @__PURE__ */ s(ne, { value: o, onChange: i, "aria-label": t }) });
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
  return /* @__PURE__ */ s(M, { label: e, hint: o ? /* @__PURE__ */ s("span", { className: "bq-hint", children: o }) : void 0, children: /* @__PURE__ */ s(
    "input",
    {
      className: "input",
      type: "number",
      min: 0,
      max: i,
      step: r ? 1 : "any",
      value: t === void 0 ? "" : String(t),
      onChange: (u) => {
        const c = u.target.value.trim();
        if (c === "") return n(a ? void 0 : 0);
        const m = Math.max(0, r ? Math.round(Number(c)) : Number(c));
        Number.isFinite(m) && n(i === void 0 ? m : Math.min(i, m));
      }
    }
  ) });
}
function V({
  ctx: e,
  label: t,
  hint: n,
  value: o,
  onChange: i
}) {
  return /* @__PURE__ */ h("div", { className: "bq-field", children: [
    /* @__PURE__ */ s("span", { className: "field-label", children: t }),
    /* @__PURE__ */ s(
      ie,
      {
        projectId: e.projectId,
        value: ge(o, e.lang),
        onChange: (a) => i(ye(a))
      }
    ),
    /* @__PURE__ */ s("span", { className: "bq-hint", children: n })
  ] });
}
function _({
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
function P(e, t, n) {
  if (n < 0 || n >= e.length) return;
  const [o] = e.splice(t, 1);
  e.splice(n, 0, o);
}
function U(e, t, n) {
  const o = /* @__PURE__ */ new Set(), i = e?.documents?.[w];
  for (const a of i ?? []) {
    const r = a;
    if (r.id !== t.id)
      for (const u of r.items ?? []) o.add(u.id);
  }
  return t.items.forEach((a, r) => {
    r !== n && o.add(a.id);
  }), o;
}
function we({
  ctx: e,
  category: t,
  onOpen: n
}) {
  const { t: o, edit: i, project: a, lang: r } = e, u = S(a), c = H(t), m = () => {
    const l = A(), d = me("item", U(a, t, -1));
    i(
      (p) => {
        p.items.push({
          id: d,
          name: { key: l },
          icon: "CHEST",
          price: 0,
          discount: 0,
          actions: null,
          rest: {}
        });
      },
      [{ key: l, lang: r, value: o("newItemName") }]
    ), n(d);
  };
  return /* @__PURE__ */ h(O, { children: [
    /* @__PURE__ */ h("section", { className: "bq-section", children: [
      /* @__PURE__ */ s("span", { className: "bq-h", children: o("category") }),
      /* @__PURE__ */ s(K, { ctx: e, label: o("name"), node: t.name }),
      /* @__PURE__ */ s(
        V,
        {
          ctx: e,
          label: o("icon"),
          hint: o("iconHint"),
          value: t.icon,
          onChange: (l) => i((d) => {
            d.icon = l;
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
            onChange: (l) => i((d) => {
              d.discount = l ?? 0;
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
            onChange: (l) => i((d) => {
              d.order = l;
            })
          }
        )
      ] })
    ] }),
    /* @__PURE__ */ h("section", { className: "bq-section", children: [
      /* @__PURE__ */ s("div", { className: "bq-head", children: /* @__PURE__ */ s("span", { className: "bq-h", children: o("items") }) }),
      t.items.length === 0 ? /* @__PURE__ */ s("span", { className: "bq-hint", children: o("noItems") }) : null,
      /* @__PURE__ */ s("ul", { className: "bq-list", children: t.items.map((l, d) => {
        const p = C(l.price, u, t.discount, l.discount);
        return /* @__PURE__ */ h("li", { className: "bq-item", children: [
          /* @__PURE__ */ h("button", { type: "button", className: "bq-item-main", onClick: () => n(c[d]), children: [
            /* @__PURE__ */ s("span", { className: "bq-item-name", children: y(a, l.name.key, r) || l.id }),
            /* @__PURE__ */ s("span", { className: "bq-item-mat", children: z(l.icon) }),
            /* @__PURE__ */ s("span", { className: "bq-price", children: B(l.price, p) })
          ] }),
          /* @__PURE__ */ s(
            _,
            {
              label: o("moveUp"),
              disabled: d === 0,
              onClick: () => i((f) => P(f.items, d, d - 1)),
              children: "↑"
            }
          ),
          /* @__PURE__ */ s(
            _,
            {
              label: o("moveDown"),
              disabled: d === t.items.length - 1,
              onClick: () => i((f) => P(f.items, d, d + 1)),
              children: "↓"
            }
          ),
          /* @__PURE__ */ s(
            _,
            {
              label: o("remove"),
              onClick: () => i((f) => {
                f.items.splice(d, 1);
              }),
              children: "×"
            }
          )
        ] }, c[d]);
      }) }),
      /* @__PURE__ */ h($, { variant: "secondary", size: "sm", className: "bq-add", onClick: m, children: [
        "+ ",
        o("addItem")
      ] })
    ] })
  ] });
}
function Ie({
  ctx: e,
  category: t,
  index: n,
  onIdChange: o
}) {
  const { t: i, edit: a, project: r } = e, u = t.items[n], c = (p) => a((f) => {
    p(f.items[n]);
  }), m = U(r, t, n).has(u.id), l = C(u.price, S(r), t.discount, u.discount), d = Object.keys(u.rest);
  return /* @__PURE__ */ h(O, { children: [
    /* @__PURE__ */ h("section", { className: "bq-section", children: [
      /* @__PURE__ */ s("span", { className: "bq-h", children: i("item") }),
      /* @__PURE__ */ s(K, { ctx: e, label: i("name"), node: u.name }),
      /* @__PURE__ */ s(
        M,
        {
          label: i("id"),
          hint: m ? /* @__PURE__ */ s("span", { className: "bq-err", children: i("idTaken") }) : /* @__PURE__ */ s("span", { className: "bq-hint", children: i("idHint") }),
          children: /* @__PURE__ */ s(
            "input",
            {
              className: "input bq-mono",
              value: u.id,
              onChange: (p) => {
                const f = ue(p.target.value);
                c((b) => {
                  b.id = f;
                }), o(f);
              }
            }
          )
        }
      ),
      /* @__PURE__ */ s(
        V,
        {
          ctx: e,
          label: i("itemIcon"),
          hint: i("itemIconHint"),
          value: u.icon,
          onChange: (p) => c((f) => {
            f.icon = p;
          })
        }
      ),
      /* @__PURE__ */ h("div", { className: "bq-row", children: [
        /* @__PURE__ */ s(
          N,
          {
            label: i("price"),
            value: u.price,
            onChange: (p) => c((f) => {
              f.price = p ?? 0;
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
            value: u.discount,
            onChange: (p) => c((f) => {
              f.discount = p ?? 0;
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
        ee,
        {
          projectId: e.projectId,
          value: u.actions ?? [],
          onChange: (p) => c((f) => {
            f.actions = p;
          })
        }
      )
    ] }),
    d.length > 0 ? /* @__PURE__ */ s("span", { className: "bq-hint", children: i("kept", { fields: d.join(", ") }) }) : null
  ] });
}
const Ce = {
  kind: w,
  dir: E,
  schemaVersion: D,
  // A category's id names it in the menus (`category` of shop_category): typed by hand.
  projects: [le],
  // The catalogue (SDK 1.9): one entry and one page, off the board, its settings on top.
  // The categories stay in boutique_categories/ at the folder's root, the only place read.
  collection: { labels: ae },
  label: j.en,
  labels: j,
  component: ke,
  title: (e, { project: t, lang: n }) => F(y(t, e.name.key, n)),
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
  parse: (e, t) => pe(e, t),
  serialize: (e, t) => he(e, t),
  ltextSites: (e) => be(e),
  board: {
    size: { width: 260, height: 48 },
    accent: qe,
    items: (e, { project: t, lang: n }) => ve(e, t, n),
    summary: (e, { lang: t }) => xe(e, t)
  }
};
function Se(e, t, n) {
  const o = n.placeholders, i = t < Math.round(e), a = o?.[i ? "discount_price_display" : "price_display"], r = typeof a == "string" && a.trim() ? a : i ? "&m%old_price% &f%price% tokens (-%discount_percent%%)" : "%price% tokens", u = Math.round(e);
  return r.replaceAll("%old_price%", String(u)).replaceAll("%discount_percent%", String(u <= 0 ? 0 : Math.round((u - t) * 100 / u))).replaceAll("%price%", String(t));
}
function _e(e) {
  return [...e].sort((t, n) => {
    const o = t.order ?? Number.POSITIVE_INFINITY, i = n.order ?? Number.POSITIVE_INFINITY;
    return o !== i ? o - i : t.id.localeCompare(n.id);
  });
}
function Ae(e, t) {
  if (t !== "boutique:categories" && t !== "boutique:items") return null;
  const n = e.documents?.[w];
  if (!n || n.length === 0) return null;
  const o = e.defaultLang ?? "fr", i = R(e), a = S(e), r = _e(n), u = (c) => c.items.map((m) => {
    const l = g(m.price), d = C(l, a, c.discount, m.discount);
    return {
      id: m.id,
      name: y(e, m.name.key, o),
      icon: m.icon,
      category: c.id,
      price: l,
      final_price: d,
      price_display: Se(l, d, i),
      lore: ""
    };
  });
  return t === "boutique:items" ? r.flatMap(u) : r.map((c) => ({
    id: c.id,
    name: y(e, c.name.key, o),
    icon: c.icon,
    items: u(c)
  }));
}
const He = {
  id: "boutique",
  displayName: "Boutique",
  schemaVersion: "boutique/v1",
  sdk: oe,
  sidebar: { icon: () => null, label: "Boutique", to: "/m/boutique" },
  // Categories are Documents of the folder; the settings are a screen the plugin declares.
  routes: [],
  documents: [Ce],
  localPreviewRows: Ae,
  dataSourceModels: {
    "boutique:items": "boutique:item",
    "boutique:categories": "boutique:category"
  }
}, Pe = { ...He, sdk: "1.9.0" };
export {
  Pe as default
};
