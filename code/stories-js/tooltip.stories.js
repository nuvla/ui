// import * as React from "react";
import { tooltip, sourceCode } from "../stories-output-js/sixsq.nuvla.ui.stories.components.tooltip_stories.js";
// import { test } from '@storybook/test';
import { within, userEvent, waitFor, fn, expect } from '@storybook/test';

export default {
  title: 'Utils/with-tooltip',
  component: tooltip,
  tags: ['autodocs'],
  argTypes: {
    tooltipText: {
      control: 'text',
      description: 'Tooltip text'
    }
  },
  parameters: {
    componentSubtitle: "Utility to add a tooltip to an existing component",
    docs: {
      description: {component: "Tooltip utility"},
      source: {code: sourceCode}
    }
  }
};

export const Simple = {
  args: {
    tooltipText: 'Simple tooltip'
  },
  play: async ({ canvasElement }) => {
    const body = within(canvasElement.parentNode);

    const testElement = body.getByTestId('test-element');

    await userEvent.hover(testElement, {
      delay: 100,
    });

    await expect(body.getByTestId('tooltip-content')).toBeInTheDocument();
  },
  tags: ['tooltip-test']
};

/* test('Additional test', async () => {
  const { container } = render('<tooltip />');
  await Simple.play({ canvasElement: container });
});
*/

export const LongText = {
  args: {
    tooltipText: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam elit neque, dapibus ut eros sed, sagittis pulvinar erat. Suspendisse risus eros, aliquet ac vulputate ut, lacinia ut risus. Pellentesque vel arcu est. Nam in gravida ex, eu rhoncus velit. Nunc id eros eros. Etiam quis diam eu ipsum viverra pulvinar. Nam a eleifend leo, ac facilisis tortor. In elementum id erat id condimentum. Sed aliquam, sem vitae rhoncus ultricies, lorem tellus placerat ipsum, tristique commodo lorem ipsum at elit. Donec faucibus euismod orci, ac sagittis leo dignissim ut. Cras sit amet felis ante. Proin sed ante sit amet lacus sagittis hendrerit vitae nec dolor. Vestibulum eget urna a lacus accumsan sollicitudin vel mollis massa. Suspendisse dictum sapien vel mattis ultrices. Duis felis neque, blandit et porta eget, efficitur vitae risus. Donec pharetra ut nisi eget ullamcorper. Aliquam vel molestie quam, sodales gravida ex. Integer sit amet justo ac augue malesuada hendrerit luctus et ex. Morbi metus nulla, imperdiet eget diam a, pharetra fermentum dui. Nam molestie eros tellus, convallis lobortis elit tristique ac. Integer et massa dolor. Ut dignissim id massa non dapibus. Nunc id ligula commodo nibh egestas semper vel quis dui. Sed non fringilla ex.'
  },
};

