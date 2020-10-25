<script>
  import {slide} from "svelte/transition";
  import Textfield from "@smui/textfield";

  export let value = "";
  export let error = false;
  export let label = "Введите текст...";
  export let type = "text";

  $: smuiError = error ? true : false;
</script>

<style lang="sass">
  @import "../theme/colors"

  .textfield-container
    font-size: .75rem
    margin: .25rem .5rem
    color: $color_error_red

    &-error
      padding: .25rem

    :global(*) 
      font-family: defaultFont

    :global(label)
      width: 100%

      &.error :global(input)
        color: $mdc-theme-error

      &:not(.error) :global(input:focus)
        color: $mdc-theme-primary

      :global(.mdc-text-field__input)
        transition: color .3s ease
        color: $mdc-theme-secondary
</style>

<div class="textfield-container">
  <Textfield
    class={error ? 'error' : ''}
    variant="standard"
    {type}
    bind:value
    invalid={smuiError}
    {label} />
  {#if error}
    <p class="textfield-container-error" transition:slide>
      {error}
    </p>
  {/if}
</div>
