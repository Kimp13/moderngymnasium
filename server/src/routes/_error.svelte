<script>
  import Title from "Title.svelte";

  export let status;
  export let error;

  let audio;

  const dev = process.env.NODE_ENV === "development";
</script>

<style lang="scss">
  h1,
  p {
    margin: 0;
    text-align: center;
  }

  h1 {
    font-size: 2.8em;
    font-weight: 700;
    margin: 0 0 0.5em 0;
  }

  p {
    margin: 1em auto;
  }
  
  audio {
    display: block;
    margin: .25rem auto;
  }

  @media (min-width: 480px) {
    h1 {
      font-size: 4em;
    }
  }
</style>

<Title caption={status} />

<h1>{status}?</h1>

<p>{error.message}</p>

{#if dev && error.stack}
  <pre>{error.stack}</pre>
{/if}

{#if status === 404}
  <audio
    src="/audio/404.mp3"
    controls
    loop />
{/if}
