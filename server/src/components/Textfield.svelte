<script>
  import { slide } from "svelte/transition";
  import { TextField } from "svelte-materialify/src";

  export let error = false,
    clearable = false;
  export let value = "",
    type = "text";
  export let label, placeholder;
  export let filled, outlined, solo;
  export let counter;

  let focused;

  $: smError = error ? true : false;
</script>

<style lang="sass">
  @import "colors"

  .textfield-container
    font-size: .75rem
    margin: .5rem
    color: $color-error

    &-error
      padding: .25rem

    :global
      *
        font-family: defaultFont

      input
        color: var(--theme-text-secondary)
        transition: color .3s ease

  :global
    .textfield-container:not(.error).focused
      .s-input
        color: $color-green !important
        caret-color: $color-green !important

      .s-text-field__wrapper::before
        border-color: $color-green

      input
        color: $color-green

    .textfield-container.error
      .s-input
        color: $color-error !important
        caret-color: $color-error !important

      .s-text-field__wrapper::before
        border-color: $color-error

      input
        color: $color-error
</style>

<div
  class="
  textfield-container
  {smError ? 'error' : ''}
  {focused ? 'focused' : ''}">
  <TextField
    bind:value
    on:focus={() => focused = true}
    on:blur={() => focused = false}
    {placeholder}
    {counter}
    maxlength={counter}
    {type}
    {clearable}
    {filled}
    {outlined}
    {solo}>
    {#if label}{label}{/if}
  </TextField>
  {#if smError}
    <p class="textfield-container-error" transition:slide>{error}</p>
  {/if}
</div>
