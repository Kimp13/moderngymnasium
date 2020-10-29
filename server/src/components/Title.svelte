<script>
  export let caption;
  let title;

  if (caption) {
    title = `${caption} • Гимназия в лучах славы`;
  } else {
    title = 'Гимназия в лучах славы';
  }
</script>

<svelte:head>
  <title>
    {title}
  </title>
</svelte:head>