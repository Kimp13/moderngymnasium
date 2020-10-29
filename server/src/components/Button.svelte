<script>
  import { createEventDispatcher } from "svelte";

  import { Button, Icon } from "svelte-materialify/src";

  let className, icon, label, outlined, depressed, fab;
  export { className as class, icon, label, outlined, depressed, fab };
  export let disabled = false;

  const dispatch = createEventDispatcher();

  const dispatchClick = (e) => dispatch("click", e);
</script>

<style lang="scss">
  @import "colors";

  :global {
    .button-component {
      &.s-btn {
        background-color: var(--button-color, $color-green);
        color: var(--button-background-color, white);

        &:hover {
          cursor: pointer;
        }
      }

      &-icon {
        display: inline-flex;
        justify-content: center;
        align-items: center;
        width: 1.1rem;
        height: 1.1rem;
        color: black;

        &.alone {
          width: 1.25rem;
          height: 1.25rem;
        }

        &:not(.alone) {
          margin-right: .4rem;
        }
      }

      &-label {
        font-weight: 700;
        font-family: defaultFont, sans-serif;
        color: white;
      }
    }
  }
</style>

<Button
  class="button-component {className || ''}"
  {disabled}
  {outlined}
  {depressed}
  {fab}
  on:click={dispatchClick}>
  {#if icon}
    <Icon path={icon} class="button-component-icon {label ? '' : 'alone'}" />
  {/if}
  {#if label}{label}{/if}
</Button>
