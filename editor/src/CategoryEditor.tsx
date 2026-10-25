import {
  ActionChainEditor,
  type ActionChainWire,
  Button,
  type DocumentEditorProps,
  ItemInput,
  MiniMessageInput,
  SortableList,
  type TextWrite,
  injectModuleStyles,
  newTextKey,
  selectDocumentItem,
  textValue,
  updateDocument,
  useContentLang,
  useProject,
} from "@artisan/editor-sdk";
import { type ReactNode, useEffect, useState } from "react";
import { priceLabel, wholeShopDiscount } from "./board";
import { type T, useT } from "./i18n";
import {
  type Category,
  KIND,
  type ShopItem,
  type Text,
  finalPrice,
  freeKey,
  iconMaterial,
  iconToSpec,
  itemRowIds,
  specToIcon,
  typedKey,
} from "./model";
import { SHOP_CSS } from "./styles";

/** One edit of the category: a copy changed by `recipe`, committed with the texts it sets. */
type Edit = (recipe: (draft: Category) => void, texts?: TextWrite[]) => void;

interface Ctx {
  projectId: string;
  project: unknown;
  lang: string;
  t: T;
  edit: Edit;
}

/**
 * The view of a `boutique_category` in the Properties panel: the category (name, icon,
 * discount, position) and its articles, then one article at a time. An article row selected
 * on the board opens it here, as a quest of a quest group does.
 */
export function CategoryEditor({ projectId, documentId, document, item }: DocumentEditorProps) {
  injectModuleStyles("boutique", SHOP_CSS);
  const category = document as Category;
  const project = useProject(projectId);
  const lang = useContentLang();
  const t = useT();
  const [openRow, setOpenRow] = useState<string | null>(item ?? null);
  useEffect(() => {
    setOpenRow(item ?? null);
  }, [item]);
  const open = (row: string | null) => {
    setOpenRow(row);
    selectDocumentItem({ documentId, item: row });
  };

  const edit: Edit = (recipe, texts) => {
    const next = structuredClone(category) as Category;
    recipe(next);
    updateDocument({ projectId, kind: KIND, id: documentId, doc: next, texts });
  };
  const ctx: Ctx = { projectId, project, lang, t, edit };

  const index = openRow ? itemRowIds(category).indexOf(openRow) : -1;
  if (index >= 0) {
    return (
      <div className="bq">
        <Button variant="ghost" size="sm" className="bq-back" onClick={() => open(null)}>
          ← {t("back")}
        </Button>
        <ItemView
          ctx={ctx}
          category={category}
          index={index}
          onIdChange={(id) => setOpenRow(id)}
        />
      </div>
    );
  }
  return (
    <div className="bq">
      <CategoryView ctx={ctx} category={category} onOpen={open} />
    </div>
  );
}

// ------------------------------------------------------------------ fields

function Field({
  label,
  children,
  hint,
}: { label: string; children: ReactNode; hint?: ReactNode }) {
  return (
    // biome-ignore lint/a11y/noLabelWithoutControl: the control is the child
    <label className="bq-field">
      <span className="field-label">{label}</span>
      {children}
      {hint}
    </label>
  );
}

function TextField({ ctx, label, node }: { ctx: Ctx; label: string; node: Text }) {
  const value = textValue(ctx.project, node.key, ctx.lang);
  const write = (v: string) => ctx.edit(() => {}, [{ key: node.key, lang: ctx.lang, value: v }]);
  return (
    <Field label={`${label} · ${ctx.lang}`}>
      <MiniMessageInput value={value} onChange={write} aria-label={label} />
    </Field>
  );
}

function NumberField({
  label,
  value,
  onChange,
  hint,
  max,
  optional,
  integer,
}: {
  label: string;
  value: number | undefined;
  onChange: (v: number | undefined) => void;
  hint?: string;
  max?: number;
  optional?: boolean;
  /** Whole numbers only: the plugin reads discounts as integers. */
  integer?: boolean;
}) {
  return (
    <Field label={label} hint={hint ? <span className="bq-hint">{hint}</span> : undefined}>
      <input
        className="input"
        type="number"
        min={0}
        max={max}
        step={integer ? 1 : "any"}
        value={value === undefined ? "" : String(value)}
        onChange={(e) => {
          const raw = e.target.value.trim();
          if (raw === "") return onChange(optional ? undefined : 0);
          const n = Math.max(0, integer ? Math.round(Number(raw)) : Number(raw));
          if (Number.isFinite(n)) onChange(max === undefined ? n : Math.min(max, n));
        }}
      />
    </Field>
  );
}

function IconField({
  ctx,
  label,
  hint,
  value,
  onChange,
}: {
  ctx: Ctx;
  label: string;
  hint: string;
  value: unknown;
  onChange: (icon: unknown) => void;
}) {
  return (
    <div className="bq-field">
      <span className="field-label">{label}</span>
      <ItemInput
        projectId={ctx.projectId}
        value={iconToSpec(value, ctx.lang)}
        onChange={(spec) => onChange(specToIcon(spec))}
      />
      <span className="bq-hint">{hint}</span>
    </div>
  );
}

function IconButton({
  label,
  onClick,
  disabled,
  children,
}: { label: string; onClick: () => void; disabled?: boolean; children: ReactNode }) {
  return (
    <button
      type="button"
      className="bq-icon-btn"
      title={label}
      aria-label={label}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  );
}

function move<X>(list: X[], from: number, to: number) {
  if (to < 0 || to >= list.length) return;
  const [entry] = list.splice(from, 1);
  list.splice(to, 0, entry!);
}

/** Every article id of the shop's other categories and of this one but `index`. */
function takenIds(project: unknown, category: Category, index: number): Set<string> {
  const out = new Set<string>();
  const docs = (project as { documents?: Record<string, unknown[]> } | undefined)?.documents?.[
    KIND
  ];
  for (const doc of docs ?? []) {
    const other = doc as Category;
    if (other.id === category.id) continue;
    for (const it of other.items ?? []) out.add(it.id);
  }
  category.items.forEach((it, i) => {
    if (i !== index) out.add(it.id);
  });
  return out;
}

// ------------------------------------------------------------------ category

function CategoryView({
  ctx,
  category,
  onOpen,
}: { ctx: Ctx; category: Category; onOpen: (row: string) => void }) {
  const { t, edit, project, lang } = ctx;
  const whole = wholeShopDiscount(project);
  const rowIds = itemRowIds(category);

  const addItem = () => {
    const nameKey = newTextKey();
    const id = freeKey("item", takenIds(project, category, -1));
    edit(
      (c) => {
        c.items.push({
          id,
          name: { key: nameKey },
          icon: "CHEST",
          price: 0,
          discount: 0,
          actions: null,
          rest: {},
        });
      },
      [{ key: nameKey, lang, value: t("newItemName") }],
    );
    onOpen(id);
  };

  return (
    <>
      <section className="bq-section">
        <span className="bq-h">{t("category")}</span>
        <TextField ctx={ctx} label={t("name")} node={category.name} />
        <IconField
          ctx={ctx}
          label={t("icon")}
          hint={t("iconHint")}
          value={category.icon}
          onChange={(icon) =>
            edit((c) => {
              c.icon = icon;
            })
          }
        />
        <div className="bq-row">
          <NumberField
            label={t("discount")}
            hint={t("categoryDiscountHint")}
            max={100}
            integer
            value={category.discount}
            onChange={(v) =>
              edit((c) => {
                c.discount = v ?? 0;
              })
            }
          />
          <NumberField
            label={t("order")}
            hint={t("orderHint")}
            optional
            integer
            value={category.order}
            onChange={(v) =>
              edit((c) => {
                c.order = v;
              })
            }
          />
        </div>
      </section>

      <section className="bq-section">
        <div className="bq-head">
          <span className="bq-h">{t("items")}</span>
        </div>
        {category.items.length === 0 ? <span className="bq-hint">{t("noItems")}</span> : null}
        <SortableList
          ids={rowIds}
          onMove={(from, to) => edit((c) => move(c.items, from, to))}
          className="bq-list"
          itemClassName="bq-item"
        >
          {(i, handle) => {
            const item = category.items[i]!;
            const paid = finalPrice(item.price, whole, category.discount, item.discount);
            return (
              <>
                {handle}
                <button type="button" className="bq-item-main" onClick={() => onOpen(rowIds[i]!)}>
                  <span className="bq-item-name">
                    {textValue(project, item.name.key, lang) || item.id}
                  </span>
                  <span className="bq-item-mat">{iconMaterial(item.icon)}</span>
                  <span className="bq-price">{priceLabel(item.price, paid)}</span>
                </button>
                <IconButton
                  label={t("remove")}
                  onClick={() =>
                    edit((c) => {
                      c.items.splice(i, 1);
                    })
                  }
                >
                  ×
                </IconButton>
              </>
            );
          }}
        </SortableList>
        <Button variant="secondary" size="sm" className="bq-add" onClick={addItem}>
          + {t("addItem")}
        </Button>
      </section>
    </>
  );
}

// ------------------------------------------------------------------ article

function ItemView({
  ctx,
  category,
  index,
  onIdChange,
}: {
  ctx: Ctx;
  category: Category;
  index: number;
  onIdChange: (id: string) => void;
}) {
  const { t, edit, project } = ctx;
  const item = category.items[index]!;
  const set = (recipe: (it: ShopItem) => void) =>
    edit((c) => {
      recipe(c.items[index]!);
    });
  const taken = takenIds(project, category, index).has(item.id);
  const paid = finalPrice(item.price, wholeShopDiscount(project), category.discount, item.discount);
  const kept = Object.keys(item.rest);

  return (
    <>
      <section className="bq-section">
        <span className="bq-h">{t("item")}</span>
        <TextField ctx={ctx} label={t("name")} node={item.name} />
        <Field
          label={t("id")}
          hint={
            taken ? (
              <span className="bq-err">{t("idTaken")}</span>
            ) : (
              <span className="bq-hint">{t("idHint")}</span>
            )
          }
        >
          <input
            className="input bq-mono"
            value={item.id}
            onChange={(e) => {
              const id = typedKey(e.target.value);
              set((it) => {
                it.id = id;
              });
              onIdChange(id);
            }}
          />
        </Field>
        <IconField
          ctx={ctx}
          label={t("itemIcon")}
          hint={t("itemIconHint")}
          value={item.icon}
          onChange={(icon) =>
            set((it) => {
              it.icon = icon;
            })
          }
        />
        <div className="bq-row">
          <NumberField
            label={t("price")}
            value={item.price}
            onChange={(v) =>
              set((it) => {
                it.price = v ?? 0;
              })
            }
          />
          <NumberField
            label={t("discount")}
            hint={t("itemDiscountHint")}
            max={100}
            integer
            value={item.discount}
            onChange={(v) =>
              set((it) => {
                it.discount = v ?? 0;
              })
            }
          />
        </div>
        <span className="bq-ok">{t("paid", { price: String(paid) })}</span>
      </section>

      <section className="bq-section">
        <span className="bq-h">{t("actions")}</span>
        <span className="bq-hint">{t("actionsHint")}</span>
        <ActionChainEditor
          projectId={ctx.projectId}
          // A legacy Action DSL string is read by the host's editor and written back keyed.
          value={(item.actions ?? []) as ActionChainWire}
          onChange={(steps) =>
            set((it) => {
              it.actions = steps;
            })
          }
        />
      </section>
      {kept.length > 0 ? (
        <span className="bq-hint">{t("kept", { fields: kept.join(", ") })}</span>
      ) : null}
    </>
  );
}
