// import * as React from "react";
import { overflowTooltip } from "../stories-output-js/sixsq.nuvla.ui.stories.components.overflow_tooltip_stories.js";
// import { test } from '@storybook/test';
import { within, userEvent, waitFor } from '@storybook/test';
import { expect } from '@storybook/jest';

export default {
  title: 'Utils/with-overflow-tooltip',
  component: overflowTooltip,
  tags: ['autodocs'],
  argTypes: {
    tooltipText: {
      control: 'text',
      description: 'Tooltip text'
    },
    overflowTooltip: {
      control: 'boolean',
      description: 'Whether the tooltip is meant to show overflowed content'
    }
  },
};

export const WithOverflow = {
  args: {
    tooltipText: 'Tooltip showing overflowing context',
    overflowTooltip: true
  },
  play: async ({ canvasElement }) => {
    const body = within(canvasElement.parentNode);

    const testElement = body.getByTestId('test-element');

    await userEvent.hover(testElement, {
      delay: 100,
    });

    await expect(body.getByTestId('tooltip-content')).toBeInTheDocument();
  },
}

export const NoOverflow = {
  args: {
    tooltipText: 'This text should not be shown',
    overflowTooltip: false
  },
  play: async ({ canvasElement }) => {
    const body = within(canvasElement.parentNode);

    const testElement = body.getByTestId('test-element');

    await userEvent.hover(testElement, {
      delay: 100,
    });

    await expect(body.queryByTestId('tooltip-content')).toBeNull();
  },
}
