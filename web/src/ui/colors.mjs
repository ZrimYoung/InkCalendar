import catalog from '../generated/catalog.mjs';

export function colorsFor(mode) {
  return catalog.colorModes.find(palette=>palette.id===mode)||catalog.colorModes[0];
}
