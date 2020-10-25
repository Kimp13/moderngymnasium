<style lang="sass">
  @import "../theme/colors"

  .loader
    display: flex
    justify-content: center
    align-items: center
    padding: .5rem 0
    width: 100%
    height: 3rem

    &-child
      width: 1rem
      height: 1rem
      margin: 0 .25rem
      border-radius: 50%
      background: $mdc-theme-primary
      animation-duration: 2s
      animation-iteration-count: infinite
      animation-name: animate

  @keyframes animate
    from
      transform: none
    20%
      transform: translateY(100%)
    80%
      transform: translateY(-100%)
    to
      transform: none
</style>

<script>
  let children = [
    1, 2, 3
  ];
</script>

<div class="loader">
  {#each children as child}
    <div class="loader-child" style="animation-delay: {child * 0.1}s" />
  {/each}
</div>