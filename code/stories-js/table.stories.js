//import * as React from "react";
import { table } from "../stories-output-js/sixsq.nuvla.ui.stories.components.table_stories.js";
//import { table } from "../stories-output-js/stories.js";

export default {
  title: 'Data/Table',
  component: table,
  tags: ['autodocs'],
  argTypes: {
    numberOfColumns: { control: 'number' }
  }
};

export const OneColum = {
  args: {
    numberOfColumns: 1
  },
};

export const TwoColumns = {
  args: {
    numberOfColumns: 2
  },
};

export const ManyColumns = {
  args: {
    numberOfColumns: 10
  },
};
