<script>
  export let caption;
  let title;

  if (caption) {
    title = `${caption} • undefined`;
  } else {
    title = 'undefined';
  }
</script>

<svelte:head>
  <title>
    {title}
  </title>
</svelte:head>