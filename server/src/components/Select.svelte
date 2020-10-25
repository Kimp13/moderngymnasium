<script>
  import Select, { Option } from "@smui/select";

  let className;
  export { className as class };
  export let options = [];
  export let enhanced = true;
  export let invalid = false;
  export let label = "";
  export let value;
  export let selectedIndex;
  export let error;
</script>

<style lang="sass">
  @import "../theme/colors"

  :global(.select-container.error)
    :global(.mdc-floating-label)
      color: $mdc-theme-error
      border-bottom-color: $mdc-theme-error


  .select-container
    display: block
    width: 100%

    &:not(.error)
      :global(.mdc-select__selected-text)
        color: $mdc-theme-primary
    
    :global(.select-component)
      width: 100%
      margin: .25rem .5rem

      :global(.mdc-select__selected-text)
        transition: color .3s ease

    &-error
      font-size: .75rem
      margin: .25rem .5rem
      color: $mdc-theme-error
</style>

<div class="select-container">
  <Select
    class="select-component {className || ''} {error ? 'error' : ''}"
    {enhanced}
    {invalid}
    {label}
    bind:selectedIndex
    bind:value>
    {#each options as option}
      <Option
        value={option}
        selected={value === option}>
        {option}
      </Option>
    {/each}
  </Select>
  {#if error}
    <p class="select-container-error">
      {error}
    </p>
  {/if}
</div>
