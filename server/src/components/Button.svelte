<script>
  import { createEventDispatcher } from "svelte";

  import Button, { Icon, Label } from "@smui/button";

  let className, icon, label;
  export { className as class, icon, label };
  export let disabled = false;
  export let variant = "raised";
  export let color = "primary";

  const dispatch = createEventDispatcher();

  const dispatchClick = (e) => dispatch("click", e);
</script>

<style lang="sass">
  :global(.button-component)
    :global(.button-component-icon)
      display: inline-flex
      justify-content: center
      align-items: center
      width: 1.1rem
      height: 1.1rem
      color: white

    :global(.button-component-icon.alone)
      margin: 0
      width: 1.25rem
      height: 1.25rem

    :global(.button-component-label)
      font-weight: 700
      font-family: defaultFont, sans-serif
      color: white
</style>

<Button
  class="button-component {className || ''}"
  {disabled}
  {color}
  {variant}
  on:click={dispatchClick}>
  {#if icon}
    <Icon class="material-icons button-component-icon {label ? '' : 'alone'}">
      {icon}
    </Icon>
  {/if}
  {#if label}
    <Label class="button-component-label">{label}</Label>
  {/if}
</Button>
