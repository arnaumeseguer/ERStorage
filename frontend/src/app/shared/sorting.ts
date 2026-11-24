export type SortKey = 'name' | 'size' | 'updated';

interface Sortable {
  name: string;
  updatedAt: string;
  sizeBytes?: number;
}

export const nameCollator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });

function compareBy(key: SortKey, a: Sortable, b: Sortable): number {
  switch (key) {
    case 'size':
      return (a.sizeBytes ?? 0) - (b.sizeBytes ?? 0);
    case 'updated':
      return Date.parse(a.updatedAt) - Date.parse(b.updatedAt);
    default:
      return nameCollator.compare(a.name, b.name);
  }
}

export function sortItems<T extends Sortable>(items: T[], key: SortKey, ascending: boolean): T[] {
  const direction = ascending ? 1 : -1;
  return [...items].sort((a, b) => direction * compareBy(key, a, b));
}